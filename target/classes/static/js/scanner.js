/**
 * SmartAttend - Webcam Barcode Scanner Module using ZXing Browser
 */

class SmartScanner {
    constructor() {
        this.codeReader = null;
        this.selectedDeviceId = null;
        this.isScanning = false;
        this.lastScannedCode = null;
        this.lastScanTimestamp = 0;
        this.audioContext = null;
        this.currentMode = null; // 'registration' or 'attendance'
    }

    // Synthesize audio chime without needing external mp3 files
    playBeep(success = true) {
        try {
            if (!this.audioContext) {
                this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            }
            if (this.audioContext.state === 'suspended') {
                this.audioContext.resume();
            }

            const osc = this.audioContext.createOscillator();
            const gain = this.audioContext.createGain();
            osc.connect(gain);
            gain.connect(this.audioContext.destination);

            if (success) {
                // Pleasant ascending double-tone
                osc.type = 'sine';
                osc.frequency.setValueAtTime(880, this.audioContext.currentTime); // A5
                osc.frequency.setValueAtTime(1174.66, this.audioContext.currentTime + 0.08); // D6
                gain.gain.setValueAtTime(0.2, this.audioContext.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.25);
                osc.start();
                osc.stop(this.audioContext.currentTime + 0.25);
            } else {
                // Warning low tone
                osc.type = 'sawtooth';
                osc.frequency.setValueAtTime(320, this.audioContext.currentTime);
                gain.gain.setValueAtTime(0.2, this.audioContext.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.3);
                osc.start();
                osc.stop(this.audioContext.currentTime + 0.3);
            }
        } catch (e) {
            console.warn('Audio chime unavailable:', e);
        }
    }

    // Check if ZXing is loaded
    initReader() {
        if (this.codeReader) return true;
        try {
            if (typeof ZXing !== 'undefined') {
                this.codeReader = new ZXing.BrowserMultiFormatReader();
                return true;
            } else {
                console.error('ZXing library not found in window object');
                return false;
            }
        } catch (e) {
            console.error('Failed to instantiate ZXing reader:', e);
            return false;
        }
    }

    // Populate video input devices dropdown
    async populateCameraDevices(selectElementId) {
        const select = document.getElementById(selectElementId);
        if (!select) return;

        try {
            if (!this.initReader()) return;
            const videoInputDevices = await this.codeReader.listVideoInputDevices();
            select.innerHTML = '';

            if (videoInputDevices.length === 0) {
                const opt = document.createElement('option');
                opt.text = 'No camera found';
                select.appendChild(opt);
                return;
            }

            videoInputDevices.forEach((device, index) => {
                const option = document.createElement('option');
                option.value = device.deviceId;
                option.text = device.label || `Camera ${index + 1}`;
                select.appendChild(option);
            });

            this.selectedDeviceId = videoInputDevices[0].deviceId;
            select.addEventListener('change', (e) => {
                this.selectedDeviceId = e.target.value;
                if (this.isScanning) {
                    this.stopScan();
                    this.startScan(this.currentMode);
                }
            });
        } catch (err) {
            console.error('Error listing camera devices:', err);
        }
    }

    // Start scanner for either 'registration' or 'attendance'
    async startScan(mode = 'attendance', videoElemId = 'attendanceVideo', onScanCallback = null) {
        if (!this.initReader()) {
            alert('Barcode scanner library is loading. Please check your connection or use manual barcode entry.');
            return;
        }

        this.currentMode = mode;
        const videoElement = document.getElementById(videoElemId);
        if (!videoElement) {
            console.error('Video element not found:', videoElemId);
            return;
        }

        try {
            this.isScanning = true;
            this.lastScannedCode = null;
            this.lastScanTimestamp = 0;

            await this.codeReader.decodeFromVideoDevice(
                this.selectedDeviceId,
                videoElemId,
                (result, err) => {
                    if (result && this.isScanning) {
                        const code = result.getText().trim();
                        const now = Date.now();

                        // Debounce same card scan within 2.5 seconds, but allow different cards immediately!
                        if (code === this.lastScannedCode && (now - this.lastScanTimestamp) < 2500) {
                            return;
                        }

                        this.lastScannedCode = code;
                        this.lastScanTimestamp = now;

                        // Trigger callback
                        if (onScanCallback) {
                            onScanCallback(code);
                        }
                    }
                }
            );
        } catch (err) {
            console.error('Error starting video stream:', err);
            this.isScanning = false;
            let msg = 'Camera access error: ' + err.message;
            if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
                msg = 'Camera permission denied! Please allow webcam access in your browser address bar.';
            } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
                msg = 'No camera found on this device.';
            }
            alert(msg + '\n\nNote: Webcam access requires localhost or HTTPS.');
        }
    }

    stopScan() {
        if (this.codeReader && this.isScanning) {
            try {
                this.codeReader.reset();
            } catch (e) {
                console.warn('Error resetting code reader:', e);
            }
            this.isScanning = false;
        }
    }
}

// Export singleton instance
window.smartScanner = new SmartScanner();
