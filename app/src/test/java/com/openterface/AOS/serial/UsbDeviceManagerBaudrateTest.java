package com.openterface.AOS.serial;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class UsbDeviceManagerBaudrateTest {
    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private UsbDevice device;

    @Before
    public void setUp() {
        UsbDeviceManager.port = null;
        UsbDeviceManager.driver = null;
        context = mock(Context.class);
        preferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        when(context.getSharedPreferences("serial_settings", Context.MODE_PRIVATE)).thenReturn(preferences);
        when(preferences.getInt("preferred_baudrate", -1)).thenReturn(-1);
        when(preferences.edit()).thenReturn(editor);
        when(editor.putInt("preferred_baudrate", 57600)).thenReturn(editor);
        device = mock(UsbDevice.class);
        when(device.getVendorId()).thenReturn(0x1A86);
        when(device.getProductId()).thenReturn(0x7523);
    }

    @After
    public void tearDown() {
        UsbDeviceManager.port = null;
        UsbDeviceManager.driver = null;
    }

    private UsbDeviceManager newManager() {
        return new UsbDeviceManager(context, mock(UsbManager.class));
    }

    private void detectDevice(UsbDeviceManager manager) throws Exception {
        Method method = UsbDeviceManager.class.getDeclaredMethod("applyDeviceSpecificConfig", UsbDevice.class);
        method.setAccessible(true);
        method.invoke(manager, device);
    }

    @Test
    public void selected57600IsSavedAndSurvivesDeviceDetection() throws Exception {
        UsbDeviceManager manager = newManager();
        manager.setPreferredBaudrate(57600);
        detectDevice(manager);
        assertEquals(57600, manager.getPreferredBaudrate());
        verify(editor).putInt("preferred_baudrate", 57600);
        verify(editor).apply();
    }

    @Test
    public void saved57600IsRestoredOnAppRestart() throws Exception {
        when(preferences.getInt("preferred_baudrate", -1)).thenReturn(57600);
        UsbDeviceManager manager = newManager();
        detectDevice(manager);
        assertEquals(57600, manager.getPreferredBaudrate());
    }

    @Test
    public void originalDeviceDefaultStillAppliesWithoutSavedSelection() throws Exception {
        UsbDeviceManager manager = newManager();
        detectDevice(manager);
        assertEquals(9600, manager.getPreferredBaudrate());
    }

    @Test
    public void invalidSavedSelectionUsesDeviceDefault() throws Exception {
        when(preferences.getInt("preferred_baudrate", -1)).thenReturn(12345);
        UsbDeviceManager manager = newManager();
        detectDevice(manager);
        assertEquals(9600, manager.getPreferredBaudrate());
    }

    @Test
    public void changingBaudrateClosesPortAndQueuesNewConnection() throws Exception {
        UsbDeviceManager manager = newManager();
        UsbSerialPort port = mock(UsbSerialPort.class);
        when(port.isOpen()).thenReturn(true, true, false);
        UsbDeviceManager.port = port;
        Handler handler = mock(Handler.class);
        Field handlerField = UsbDeviceManager.class.getDeclaredField("mSerialAsyncHandler");
        handlerField.setAccessible(true);
        handlerField.set(manager, handler);
        Field connectingField = UsbDeviceManager.class.getDeclaredField("isConnecting");
        connectingField.setAccessible(true);
        connectingField.set(manager, true);
        UsbDeviceManager.OnConnectionStatusListener listener =
                mock(UsbDeviceManager.OnConnectionStatusListener.class);
        manager.setOnConnectionStatusListener(listener);

        manager.reconnectWithBaudrate(57600);

        verify(port).close();
        verify(listener).onDisconnected();
        verify(handler).post(any(Runnable.class));
        assertEquals(57600, manager.getPreferredBaudrate());
    }
}
