package com.example.testconexionusb;

import android.hardware.usb.UsbDeviceConnection;
import android.net.ParseException;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigurationDescriptor {
    private static final String TAG = ConfigurationDescriptor.class.getSimpleName();

    // Type: Indicates whether this is a read or write
    //  Matches USB_ENDPOINT_DIR_MASK for either IN or OUT
    private static final int REQUEST_TYPE = 0x80;
    // Request: GET_DESCRIPTOR = 0x06
    private static final int REQUEST = 0x06;
    // Value: Descriptor Type (High) and Index (Low)
    //  Configuration Descriptor = 0x2
    //  Descriptor Index = 0x0 (First configuration)
    private static final int REQ_VALUE = 0x200;
    private static final int REQ_INDEX = 0x00;
    private static final int LENGTH = 9;
    private static final int TIMEOUT = 2000;

    /**
     * Request the active configuration descriptor through the USB device connection.
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static ConfigurationDescriptor fromDeviceConnection(UsbDeviceConnection connection)
            throws IllegalArgumentException, ParseException {
        //Create a sufficiently large buffer for incoming data
        byte[] buffer = new byte[LENGTH];

        connection.controlTransfer(REQUEST_TYPE, REQUEST, REQ_VALUE, REQ_INDEX,
                buffer, LENGTH, TIMEOUT);

        //Do a short read to determine descriptor length
        int totalLength = headerLengthCheck(buffer);
        //Obtain the full descriptor
        buffer = new byte[totalLength];
        connection.controlTransfer(REQUEST_TYPE, REQUEST, REQ_VALUE, REQ_INDEX,
                buffer, totalLength, TIMEOUT);

        return parseResponse(buffer);
    }

    /**
     * Verify the buffers represents a proper descriptor, and return the length
     */
    private static int headerLengthCheck(byte[] buffer) {
        if (buffer[0] != UsbHelper.DESC_SIZE_CONFIG || buffer[1] != UsbHelper.DESC_TYPE_CONFIG) {
            throw new IllegalArgumentException("Invalid configuration descriptor");
        }

        // Parse configuration descriptor header
        int totalLength = (buffer[3] & 0xFF) << 8;
        totalLength += (buffer[2] & 0xFF);

        return totalLength;
    }

    /**
     * Parse the USB configuration descriptor response per the USB Specification.
     *
     * @param buffer Raw configuration descriptor from the device.
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private static ConfigurationDescriptor parseResponse(byte[] buffer) throws ParseException {
        int totalLength = headerLengthCheck(buffer);

        // Interface count
        int interfaceCount = (buffer[4] & 0xFF);
        // Configuration attributes
        int attributes = (buffer[7] & 0xFF);
        // Power is given in 2mA increments
        int maxPower = (buffer[8] & 0xFF) * 2;

        ConfigurationDescriptor descriptor =
                new ConfigurationDescriptor(interfaceCount, attributes, maxPower);

        // The rest of the descriptor is interfaces and endpoints
        int index = UsbHelper.DESC_SIZE_CONFIG;
        InterfaceInfo nextInterface = null;
        while (index < totalLength) {
            if (index >= buffer.length) {
                throw new ParseException("Prematurely reached descriptor end.");
            }

            //Read length and type
            int len = (buffer[index] & 0xFF);
            int type = (buffer[index + 1] & 0xFF);
            switch (type) {
                case UsbHelper.DESC_TYPE_INTERFACE:
                    if (len < UsbHelper.DESC_SIZE_INTERFACE) {
                        throw new ParseException("Invalid interface descriptor length.");
                    }

                    int intfId = (buffer[index + 2] & 0xFF);
                    int intfEndpointCount = (buffer[index + 4] & 0xFF);
                    int intfClass = (buffer[index + 5] & 0xFF);
                    int intfSubclass = (buffer[index + 6] & 0xFF);
                    int intfProtocol = (buffer[index + 7] & 0xFF);

                    nextInterface =
                            new InterfaceInfo(intfId, intfEndpointCount, intfClass, intfSubclass, intfProtocol);
                    descriptor.interfaces.add(nextInterface);

                    break;
                case UsbHelper.DESC_TYPE_ENDPOINT:
                    if (len < UsbHelper.DESC_SIZE_ENDPOINT) {
                        throw new ParseException("Invalid endpoint descriptor length.");
                    }

                    int endpointAddr = ((buffer[index + 2] & 0xFF));
                    int endpointAttrs = (buffer[index + 3] & 0xFF);
                    int maxPacketSize = (buffer[index + 5] & 0xFF) << 8;
                    maxPacketSize += (buffer[index + 4] & 0xFF);

                    int epInterval = (buffer[index + 6] & 0xFF);

                    nextInterface.endpoints.add(
                            new EndpointInfo(endpointAddr, endpointAttrs, maxPacketSize, epInterval));
                    break;
            }
            //Advance to next descriptor
            index += len;
        }

        return descriptor;
    }

    // Descriptor fields
    public final int interfaceCount;
    public final boolean busPowered;
    public final boolean selfPowered;
    public final boolean remoteWakeup;
    public final int maxPower;
    public List<InterfaceInfo> interfaces;

    public ConfigurationDescriptor(int interfaceCount, int attributes, int maxPower) {
        this.interfaceCount = interfaceCount;
        this.interfaces = new ArrayList<>(interfaceCount);
        this.busPowered = (attributes & UsbHelper.ATTR_BUSPOWER) == UsbHelper.ATTR_BUSPOWER;
        this.selfPowered = (attributes & UsbHelper.ATTR_SELFPOWER) == UsbHelper.ATTR_SELFPOWER;
        this.remoteWakeup = (attributes & UsbHelper.ATTR_REMOTEWAKE) == UsbHelper.ATTR_REMOTEWAKE;
        this.maxPower = maxPower;
    }

    /**
     * Return a printable description of the connected device.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Configuration Descriptor:\n");
        sb.append(interfaceCount)
                .append(" Interfaces\n");
        sb.append(String.format("Attributes:%s%s%s\n",
                busPowered ? " BusPowered" : "",
                selfPowered ? " SelfPowered" : "",
                remoteWakeup ? " RemoteWakeup" : ""));
        sb.append("Max Power: ")
                .append(maxPower)
                .append("mA\n");

        for (InterfaceInfo interfaceInfo : interfaces) {
            sb.append(interfaceInfo == null ? "null\n" : interfaceInfo);
            for (EndpointInfo endpointInfo : interfaceInfo.endpoints) {
                sb.append(endpointInfo == null ? "null\n" : endpointInfo);
            }
        }

        return sb.toString();
    }

    /**
     * Structured USB interface representation
     */
    public static class InterfaceInfo {
        public final int id;
        public final int endpointCount;
        public final int interfaceClass;
        public final int interfaceSubclass;
        public final int interfaceProtocol;
        public final List<EndpointInfo> endpoints;

        public InterfaceInfo(int id, int endpointCount,
                             int interfaceClass, int interfaceSubclass, int interfaceProtocol) {
            this.id = id;
            this.endpointCount = endpointCount;
            this.interfaceClass = interfaceClass;
            this.interfaceSubclass = interfaceSubclass;
            this.interfaceProtocol = interfaceProtocol;
            this.endpoints = new ArrayList<>(endpointCount);
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "-- Interface %d Class: %s -> Subclass: 0x%02x -> Protocol: 0x%02x, %d Endpoints\n",
                    id,
                    UsbHelper.nameForClass(interfaceClass), interfaceSubclass, interfaceProtocol,
                    endpointCount);
        }
    }

    /**
     * Structured USB endpoint representation
     */
    public static class EndpointInfo {
        public final int id;
        public final int direction;
        public final int type;
        public final int maxPacketSize;
        public final int interval;

        public EndpointInfo(int address, int attributes, int maxPacketSize, int interval) {
            //Number is lower 4 bits
            this.id = (address & 0x0F);
            //Direction is high bit
            this.direction = (address & 0x80);
            //Type is the lower two bits
            this.type = (attributes & 0x3);
            this.maxPacketSize = maxPacketSize;
            this.interval = interval;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "   -- Endpoint %d, %s %s, %d byte packets\n",
                    id,
                    UsbHelper.nameForEndpointType(type),
                    UsbHelper.nameForDirection(direction),
                    maxPacketSize);
        }
    }
}
