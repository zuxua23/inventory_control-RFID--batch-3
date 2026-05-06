package com.example.inventory_system_ht.Helper;

import android.util.Log;

import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Dto.RFIDScannerSettings;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDScanner;

/**
 * Utility untuk open/close RFID inventory mode.
 * Handles: settings, openInventory, close, error.
 *
 * Usage:
 *   RfidBulkHelper.openInventory(scanner, this, 27);   // start bulk scan
 *   RfidBulkHelper.closeInventory(scanner);            // stop
 */
public class RfidBulkHelper {

    private static final String TAG = "RfidBulkHelper";

    /**
     * Buka RFID inventory (bulk reading mode).
     *
     * @param scanner      CommScanner instance dari ScannerManager
     * @param delegate     RFIDDataDelegate (Activity yang implement onRFIDDataReceived)
     * @param powerDbm     Power level 4-30 dBm (recommended: 20-27 untuk warehouse)
     * @return true jika berhasil open
     */
    public static boolean openInventory(CommScanner scanner, RFIDDataDelegate delegate, int powerDbm) {
        if (scanner == null) {
            Log.e(TAG, "Scanner is null");
            return false;
        }

        try {
            RFIDScanner rfid = scanner.getRFIDScanner();
            if (rfid == null) {
                Log.e(TAG, "RFIDScanner is null");
                return false;
            }

            // Set delegate dulu sebelum open
            rfid.setDataDelegate(delegate);

            // Ambil settings dari SP1
            RFIDScannerSettings settings = rfid.getSettings();

            // ── RFID Bulk Reading Settings ────────────────────────────
            // CONTINUOUS1: scan terus tanpa perlu pencet trigger
            // (cocok untuk stock taking / registration bulk)
            settings.scan.triggerMode = RFIDScannerSettings.Scan.TriggerMode.CONTINUOUS1;

            // Power level (clamp 4-30)
            int safePower = Math.max(4, Math.min(30, powerDbm));
            settings.scan.powerLevelRead  = safePower;
            settings.scan.powerLevelWrite = safePower;

            // Anti double-read: prevent baca tag yang sama berulang dalam 1 sesi
            settings.scan.doubleReading = RFIDScannerSettings.Scan.DoubleReading.PREVENT1;

            // Session S1: cocok untuk inventory besar, tag stay B state 0.5-5 detik
            settings.scan.sessionFlag = RFIDScannerSettings.Scan.SessionFlag.S1;

            // Kedua polarisasi
            settings.scan.polarization = RFIDScannerSettings.Scan.Polarization.Both;

            rfid.setSettings(settings);

            // Open inventory → SP1 mulai scan UHF RFID tags
            rfid.openInventory();

            Log.d(TAG, "RFID inventory opened, power=" + safePower + " dBm");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "openInventory failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tutup RFID inventory.
     * Wajib dipanggil sebelum switch ke barcode mode atau saat activity pause.
     */
    public static void closeInventory(CommScanner scanner) {
        if (scanner == null) return;
        try {
            RFIDScanner rfid = scanner.getRFIDScanner();
            if (rfid == null) return;

            rfid.setDataDelegate(null);
            rfid.close();

            Log.d(TAG, "RFID inventory closed");
        } catch (Exception e) {
            Log.e(TAG, "closeInventory failed: " + e.getMessage());
        }
    }

    /**
     * Buka Barcode reader.
     * Catatan: RFID dan Barcode tidak bisa dibuka bersamaan!
     * Pastikan closeInventory() dipanggil dulu sebelum ini.
     */
    public static boolean openBarcode(CommScanner scanner,
                                      com.densowave.scannersdk.Listener.BarcodeDataDelegate delegate) {
        if (scanner == null) return false;
        try {
            com.densowave.scannersdk.Barcode.BarcodeScanner barcode = scanner.getBarcodeScanner();
            if (barcode == null) return false;

            barcode.setDataDelegate(delegate);
            barcode.openReader();

            Log.d(TAG, "Barcode reader opened");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "openBarcode failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tutup Barcode reader.
     */
    public static void closeBarcode(CommScanner scanner) {
        if (scanner == null) return;
        try {
            com.densowave.scannersdk.Barcode.BarcodeScanner barcode = scanner.getBarcodeScanner();
            if (barcode == null) return;

            barcode.setDataDelegate(null);
            barcode.closeReader();

            Log.d(TAG, "Barcode reader closed");
        } catch (Exception e) {
            Log.e(TAG, "closeBarcode failed: " + e.getMessage());
        }
    }

    /** Convert byte[] UII/EPC → hex string */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}