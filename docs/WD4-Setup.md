# WD4 Bluetooth HID Ring Scanner Setup Guide

This guide explains how to configure the WD4 ring scanner to work with LogiScanner on any Android phone via Bluetooth HID (keyboard wedge mode).

## Overview

The WD4 scanner can operate in multiple modes. For LogiScanner, we use **Bluetooth HID mode**, which makes the scanner act as a Bluetooth keyboard. When you scan a barcode, it types the barcode data as keystrokes directly into the active application.

## Prerequisites

- WD4 ring scanner (fully charged)
- Android phone with Bluetooth
- Barcode programming sheets (included with scanner or see below)

## Initial Setup

### 1. Reset to Factory Defaults

Before configuring, it's recommended to reset the scanner to ensure a clean configuration:

1. Scan the **"Restore All Factory Defaults"** barcode from the WD4 manual
2. Wait for the confirmation beep

### 2. Set Operating Mode to Bluetooth HID

The scanner must be configured to operate in Bluetooth HID mode:

1. Scan **"Enter Setup"** barcode
2. Scan **"Bluetooth HID"** barcode (this sets the operating mode)
3. Scan **"Exit Setup"** barcode
4. The scanner will reboot into Bluetooth HID mode

**Note**: The scanner LED will blink blue when in pairing mode.

### 3. Clear Previous Pairing (if needed)

If the scanner was previously paired with another device:

1. Scan **"Enter Setup"** barcode
2. Scan **"Clear Pairing Info"** barcode
3. Scan **"Exit Setup"** barcode

## Pairing with Android Phone

### Step 1: Put Scanner in Pairing Mode

The scanner should automatically enter pairing mode after setup. If not:
- Power cycle the scanner (hold power button to turn off, then turn back on)
- The LED should blink blue indicating pairing mode

### Step 2: Pair via Android Bluetooth Settings

1. Open **Settings** on your Android phone
2. Navigate to **Connected devices** → **Bluetooth**
3. Enable Bluetooth if not already on
4. Tap **Pair new device**
5. Look for a device named **"WD4"** or **"Symcode Scanner"** in the available devices list
6. Tap on the device name to pair
7. If prompted for a PIN, try: `0000`, `1234`, or `123456` (check WD4 manual for default PIN)
8. Wait for successful pairing confirmation

Once paired, the scanner LED should change to solid blue or turn off, and the scanner will appear as a connected keyboard in your Bluetooth devices list.

## Configure Terminating Character Suffix (Recommended)

For best results, configure the scanner to send a terminating character after each scan. This helps LogiScanner immediately detect the end of the scan:

### Set Terminator to Carriage Return (CR)

1. Scan **"Enter Setup"** barcode
2. Scan **"Set Terminating Character Suffix"** barcode
3. Scan the digits **0**, **D** (hex code for CR = 0x0D)
   - Use the barcode sheets for individual digits/characters
4. Scan **"Save"** barcode
5. Scan **"Enable Terminating Character Suffix"** barcode
6. Scan **"Exit Setup"** barcode

**Alternative**: You can also use **0A** (Line Feed, LF) or **09** (Tab) as the terminator.

## Troubleshooting

### Scanner is Dropping Characters

If you notice that scanned barcodes are incomplete or missing characters:

#### Solution 1: Disable Fast Mode
Fast mode can sometimes cause compatibility issues with Android devices.

1. Scan **"Enter Setup"** barcode
2. Scan **"Disable Fast Mode"** barcode
3. Scan **"Exit Setup"** barcode

#### Solution 2: Increase Inter-Keystroke Delay

Adding a small delay between keystrokes can help with device compatibility:

1. Scan **"Enter Setup"** barcode
2. Scan **"Set Inter-Keystroke Delay"** barcode
3. Scan **20ms** or **40ms** delay option barcode
4. Scan **"Exit Setup"** barcode

#### Solution 3: Adjust Polling Rate

1. Scan **"Enter Setup"** barcode
2. Scan **"Set Polling Rate"** barcode
3. Try **8ms** or **16ms** options
4. Scan **"Exit Setup"** barcode

### Scanner Won't Pair

- Ensure Bluetooth is enabled on your phone
- Make sure the scanner is in pairing mode (blinking blue LED)
- Clear pairing info on the scanner (see step 3 above)
- Remove any old WD4 pairing from phone's Bluetooth settings
- Restart both scanner and phone

### Scanner Paired but Not Working

- Verify the scanner is in **Bluetooth HID** mode (not SPP or other modes)
- Check that LogiScanner scanning is active (tap "Start Scanning" button)
- Try scanning a test barcode in a text editor app to verify scanner functionality
- Re-pair the device if issues persist

### Scans Not Detected in LogiScanner

- Ensure scanning is **active** in LogiScanner (button shows "Stop Scanning")
- Verify scanned barcodes are at least 6 characters long (minimum length filter)
- Check that you're scanning relatively quickly (not typing-speed slow)
- Try configuring a terminating character (CR or LF) as described above

## Testing the Scanner

Before using with LogiScanner:

1. Open any text editor or notes app on your phone
2. Tap to place cursor in the text field
3. Scan a barcode
4. You should see the barcode data appear as typed text

If this works, the scanner is correctly configured and paired.

## Daily Use

Once configured and paired:

1. Power on the WD4 scanner
2. Wait for it to automatically reconnect to your phone (solid blue LED or LED off)
3. Open LogiScanner app
4. Log in and select a scan job
5. Tap "Start Scanning"
6. Scan barcodes with the WD4 ring scanner

The scanned data will be automatically captured and submitted, just like with the MT93 hardware scanner.

## Battery and Maintenance

- **Charging**: Use the provided charging cradle/cable
- **Battery Life**: Typically 8-12 hours of continuous use
- **Storage**: Power off when not in use for extended periods
- **LED Indicators**:
  - Blinking blue: Pairing mode
  - Solid blue: Connected
  - Red: Low battery
  - Blinking red: Charging

## Barcode Reference

The WD4 comes with a barcode programming manual containing all configuration barcodes. If you've lost the manual:

- Download from manufacturer's website
- Contact support for a PDF copy
- Use standard HID barcode programming sheets

## Support

For additional support:
- Refer to the WD4 user manual
- Contact scanner manufacturer support
- Check LogiScanner GitHub issues: https://github.com/logibooks/logibooks.scanner/issues
