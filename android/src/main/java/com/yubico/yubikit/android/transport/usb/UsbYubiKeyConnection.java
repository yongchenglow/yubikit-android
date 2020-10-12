/*
 * Copyright (C) 2020 Yubico.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yubico.yubikit.android.transport.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.YubiKeyConnection;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

abstract class UsbYubiKeyConnection implements YubiKeyConnection {
    private final UsbDevice usbDevice;
    private final Semaphore lock;
    private final UsbDeviceConnection usbDeviceConnection;
    private final UsbInterface usbInterface;

    protected UsbYubiKeyConnection(UsbDevice usbDevice, Semaphore lock, UsbDeviceConnection usbDeviceConnection, UsbInterface usbInterface) throws IOException {
        try {
            if (lock.tryAcquire(200, TimeUnit.MILLISECONDS)) {
                if (!usbDeviceConnection.claimInterface(usbInterface, true)) {
                    usbDeviceConnection.close();
                    lock.release();
                    throw new IOException("Unable to claim interface");
                }
                Logger.d("Acquired connection lock for " + usbDevice.getDeviceName());
            } else {
                throw new AlreadyInUseException(usbDevice);
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted");
        }

        this.usbDevice = usbDevice;
        this.lock = lock;
        this.usbDeviceConnection = usbDeviceConnection;
        this.usbInterface = usbInterface;

        Logger.d("USB connection opened: " + this);
    }

    @Override
    public void close() {
        usbDeviceConnection.releaseInterface(usbInterface);
        usbDeviceConnection.close();
        Logger.d("Releasing connection lock for " + usbDevice.getDeviceName());
        lock.release();
        Logger.d("USB connection closed: " + this);
    }
}
