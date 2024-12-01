package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol;

class AuthData {

    final static byte[] SEQ = { 0x11, 0x22, 0x33, 0x33, 0x22, 0x11, 0x11, 0x22, 0x33, 0x33, 0x22, 0x11, 0x11, 0x22, 0x33, 0x33 };

    final static int[][] COEFFICIENTS = {
            {2,1,1,1,4,2,1,1,2,2,4,2,4,4,16,8},
            {2,1,1,1,4,2,1,1,1,1,2,1,2,2,8,4},
            {1,1,4,2,2,2,4,2,16,8,4,4,2,1,1,1},
            {1,1,4,2,1,1,2,1,8,4,2,2,2,1,1,1},
            {16,8,2,2,4,2,4,4,1,1,4,2,1,1,2,1},
            {8,4,1,1,2,1,2,2,1,1,4,2,1,1,2,1},
            {2,2,4,2,4,4,16,8,2,1,1,1,4,2,1,1},
            {1,1,2,1,2,2,8,4,2,1,1,1,4,2,1,1},
            {4,2,4,4,16,8,2,2,1,1,2,1,1,1,4,2},
            {2,1,2,2,8,4,1,1,1,1,2,1,1,1,4,2},
            {4,4,16,8,1,1,2,1,4,2,1,1,4,2,2,2},
            {2,2,8,4,1,1,2,1,4,2,1,1,2,1,1,1},
            {1,1,2,1,1,1,4,2,4,4,16,8,2,2,4,2},
            {1,1,2,1,1,1,4,2,2,2,8,4,1,1,2,1},
            {4,2,1,1,2,1,1,1,4,2,2,2,16,8,4,4},
            {4,2,1,1,2,1,1,1,2,1,1,1,8,4,2,2}
    };
}