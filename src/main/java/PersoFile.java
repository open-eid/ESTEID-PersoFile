/*
 * Copyright (c) Estonian Information System Authority
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import javax.smartcardio.*;

public class PersoFile {
	static final int LE_MAX = 256;
	static final HexFormat hex = HexFormat.of();

	public static void main(String[] args) throws CardException {
		for (CardTerminal terminal : TerminalFactory.getDefault().terminals().list()) {
			if (!terminal.isCardPresent())
				continue;
			CardChannel c = terminal.connect("*").getBasicChannel();
			// IDEMIA Card
			if (Arrays.equals(c.getCard().getATR().getBytes(), fromHex("3BDB960080B1FE451F830012233F536549440F9000F1")) || // Cosmo 8.1/8.2
					Arrays.equals(c.getCard().getATR().getBytes(), fromHex("3BDC960080B1FE451F830012233F54654944320F9000C3"))) { // Cosmo X
				// Select AID: AWP
				selectFile(c, 0x04, fromHex("A000000077010800070000FE00000100"));
				byte[] docNR = readFile(c, 0x02, new byte[] { (byte) 0xD0, 0x03 });
				System.out.format("Document NR: %s\n", new String(Arrays.copyOfRange(docNR, 2, docNR.length), StandardCharsets.UTF_8));

				// Select DF File: Perso file
				selectFile(c, 0x01, new byte[] { 0x50, 0x00 });
				readPerso(c, 16);

                // Verify PIN
                //verifyPIN(c, "1234", 0x01, 0xFF); // PIN 1
                //verifyPIN(c, "12345678", 0x02, 0xFF); // PUK
                //selectFile(c, 0x09, new byte[] { 0x3f, 0x00, (byte) 0xAD, (byte) 0xF2 });
                //verifyPIN(c, "12345", 0x85, 0xFF); // PIN 2

				// Read EF File: Auth cert
				System.out.format("Auth cert: %s\n", toHex(readCert(c, 0x09, new byte[] { 0x3f, 0x00, (byte) 0xAD, (byte) 0xF1, 0x34, 0x01 })));

				// Read EF File: Sign cert
				System.out.format("Sign cert: %s\n", toHex(readCert(c, 0x09, new byte[] { 0x3f, 0x00, (byte) 0xAD, (byte) 0xF2, 0x34, 0x1F })));
			}
			// Thales Card
			else if(Arrays.equals(c.getCard().getATR().getBytes(), fromHex("3BFF9600008031FE438031B85365494464B085051012233F1D"))) {
				// Select AID: eID Applet
				selectFile(c, 0x04, fromHex("A000000063504B43532D3135"));

				// Select DF File: Perso file
				selectFile(c, 0x08, new byte[] { (byte) 0xDF, (byte) 0xDD });
				readPerso(c, 24);

                // Verify PIN
                //verifyPIN(c, "1234", 0x81, 0x00); // PIN 1
                //verifyPIN(c, "12345", 0x82, 0x00); // PIN 2
                //verifyPIN(c, "12345678", 0x83, 0x00); // PUK

				// Read EF File: Auth cert
				System.out.format("Auth cert: %s\n", toHex(readCert(c, 0x08, new byte[] { (byte) 0xAD, (byte) 0xF1, 0x34, 0x11 })));

				// Read EF File: Sign cert
				System.out.format("Sign cert: %s\n", toHex(readCert(c, 0x08, new byte[] { (byte) 0xAD, (byte) 0xF2, 0x34, 0x21 })));
			}
		}
	}

	static void selectFile(CardChannel c, int p1, byte[] file) throws CardException {
		ResponseAPDU r = c.transmit(new CommandAPDU(0x00, 0xA4, p1, 0x0C, file));
		if (r.getSW() != 0x9000) {
			throw new CardException("Failed to select file");
		}
	}

	private static byte[] readFile(CardChannel c, int p1, byte[] file) throws CardException {
		// Select EF file
		selectFile(c, p1, file);
		// Read binary
		return c.transmit(new CommandAPDU(0x00, 0xB0, 0x00, 0x00, LE_MAX)).getData();
	}

	private static void readPerso(CardChannel c, int rows) {
		System.out.println("Perso file:");
		for (int i = 1; i < rows; ++i) {
			try {
                byte[] file = new byte[] { 0x50, (byte) i};
                if (rows > 16) {
                    file[1] = (byte) ((i / 10) * 0x10 + (i % 10));
                }
				System.out.format(" %2d: %s\n", i, new String(readFile(c, 0x02, file), StandardCharsets.UTF_8));
			} catch (CardException e) {
				System.out.format(" %2d: No info\n", i);
			}
		}
	}

	private static byte[] readCert(CardChannel c, int p1, byte[] file) throws CardException {
		byte[] buf = readFile(c, p1, file);
		if (buf[0] != (byte) 0x30 || buf[1] != (byte) 0x82)
			throw new RuntimeException("Invalid certificate data");
		byte[] data = Arrays.copyOf(buf, (((buf[2] & 0xFF) << 8) | (buf[3] & 0xFF)) + 4);
		for (int offset = buf.length; offset < data.length; offset += buf.length) {
			buf = c.transmit(new CommandAPDU(0x00, 0xB0, offset >> 8, offset & 0xFF, Math.min(data.length - offset, 0xFF))).getData();
			System.arraycopy(buf, 0, data, offset, buf.length);
		}
		return data;
	}

    private static void verifyPIN(CardChannel c, String pin, int id, int padChar) throws CardException {
        byte[] data = Arrays.copyOf(pin.getBytes(), 12);
        Arrays.fill(data, pin.length(), data.length, (byte) padChar);
        ResponseAPDU r = c.transmit(new CommandAPDU(0x00, 0x20, 0x00, id, data));
        switch (r.getSW()) {
            case 0x9000: return;
            case 0x63C2: throw  new CardException("Invalid PIN: 2 retries left");
            case 0x63C1: throw  new CardException("Invalid PIN: 1 retries left");
            case 0x6983:
            case 0x6984:
            case 0x63C0: throw  new CardException("Invalid PIN: Blocked");
            default:  throw  new CardException("Verify error");
        }
    }

	static byte[] fromHex(String data) {
		return hex.parseHex(data);
	}

	static String toHex(byte[] data) {
		return hex.formatHex(data);
	}
}
