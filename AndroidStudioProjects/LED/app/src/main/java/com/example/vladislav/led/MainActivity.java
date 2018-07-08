package com.example.vladislav.led;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.widget.ScrollView;
import static android.R.layout.*;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;

    ArrayList<String> pairedDeviceArrayList;

    ListView listViewPairedDevice;
    FrameLayout ButPanel;

    ArrayAdapter<String> pairedDeviceAdapter;
    private UUID myUUID;

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    Button Button1;
    private StringBuilder sb = new StringBuilder();

    public TextView textInfo, d10;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";

        textInfo = (TextView)findViewById(R.id.textInfo);
        d10 = (TextView)findViewById(R.id.d10);



        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);




        ButPanel = (FrameLayout) findViewById(R.id.ButPanel);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this, "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName() + " " + bluetoothAdapter.getAddress();
        textInfo.setText(String.format("Ваше устройство: %s", stInfo));

    } // END onCreate


    @Override
    protected void onStart() { // Запрос на включение Bluetooth
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup() { // Создание списка сопряжённых Bluetooth-устройств

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) { // Если есть сопряжённые устройства

            pairedDeviceArrayList = new ArrayList<>();

            for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }

            pairedDeviceAdapter = new ArrayAdapter<>(this, simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Клик по нужному устройству

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    listViewPairedDevice.setVisibility(View.GONE); // После клика скрываем список

                    String  itemValue = (String) listViewPairedDevice.getItemAtPosition(position);
                    String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес

                    BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(MAC);

                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                    myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            });
        }
    }

    @Override
    protected void onDestroy() { // Закрытие приложения
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){ // Если разрешили включить Bluetooth, тогда void setup()

            if(resultCode == Activity.RESULT_OK) {
                setup();
            }

            else { // Если не разрешили, тогда закрываем приложение

                Toast.makeText(this, "BlueTooth не включён", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth

        private BluetoothSocket bluetoothSocket = null;

        private ThreadConnectBTdevice(BluetoothDevice device) {

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() { // Коннект

            boolean success = false;

            try {
                bluetoothSocket.connect();
                success = true;
            }

            catch (IOException e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Нет коннекта, проверьте Bluetooth-устройство с которым хотите соединиться!!!", Toast.LENGTH_LONG).show();
                        listViewPairedDevice.setVisibility(View.VISIBLE);
                    }
                });

                try {
                    bluetoothSocket.close();
                }

                catch (IOException e1) {

                    e1.printStackTrace();
                }
            }

            if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        ButPanel.setVisibility(View.VISIBLE); // открываем панель с кнопками
                    }
                });

                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
            }
        }


        public void cancel() {

            Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }

    } // END ThreadConnectBTdevice:



    private class ThreadConnected extends Thread {    // Поток - приём и отправка данных

        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        private String sbprint;

        public ThreadConnected(BluetoothSocket socket) {

            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }

            catch (IOException e) {
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }


        @Override
        public void run() { // Приём данных

            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки

                    if (endOfLineIndex > 0) {

                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());

                        runOnUiThread(new Runnable() { // Вывод данных

                            @Override
                            public void run() {

                                switch (sbprint) {

                                    case "Red ON":
                                        d10.setText("Красный включен!!!");

                                        break;

                                    case "Green ON":
                                        d10.setText("Зеленый включен!!!");

                                        break;

                                    case "Blue ON":
                                        d10.setText("Синий включен!!!");

                                        break;

                                    case "Purple ON":
                                        d10.setText("Фиолетовый включен!!!");

                                        break;

                                    case "Light Blue ON":
                                        d10.setText("Голубой включен");

                                        break;

                                    case "Yellow ON":
                                        d10.setText("Желтый включен!!!");

                                        break;

                                    case "Warm White ON":
                                        d10.setText("Теплый белый включен!!!");

                                        break;

                                    case "Cold White ON":
                                        d10.setText("Холодный белый включен!!!");

                                        break;

                                    case "Lighting OFF":
                                        d10.setText("Освещение выключено!!!");

                                        break;

                                    case "Visual stimul 1":
                                        d10.setText("Режим стимуляции №1!!!");
                                        break;

                                    case "Visual stimul 2":
                                        d10.setText("Режим стимуляции №2!!!");
                                        break;

                                    case "Visual stimul 3":
                                        d10.setText("Режим стимуляции №3!!!");
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }


        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

/////////////////// Нажатие кнопок /////////////////////


/////////////////////////Красный////////////////////////////

    public void onClickBut1(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "A".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }


////////////////////////Зеленый////////////////////////////

    public void onClickBut3(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "B".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

//////////////////////Синий//////////////////////////

    public void onClickBut5(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "C".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }


    //////////////////////Фиолетовый//////////////////////////

    public void onClickBut7(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "I".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }


    //////////////////////Голубой//////////////////////////
    public void onClickBut9(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "F".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }



    //////////////////////Желтый//////////////////////////
    public void onClickBut11(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "D".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }



    //////////////////////Теплый БЕЛЫЙ//////////////////////////
    public void onClickBut13(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "W".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

    //////////////////////Холодный Белый//////////////////////////
    public void onClickBut14(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "H".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

    //////////////////////Выключить освещение//////////////////////////
    public void onClickBut15(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "R".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }
    //////////////////////Режим стимуляции 1//////////////////////////
    public void onClickBut16(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "V".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }
    //////////////////////Режим стимуляции 2//////////////////////////
    public void onClickBut17(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "S".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }
    //////////////////////Режим стимуляции 3//////////////////////////
    public void onClickBut18(View v) {

        if(myThreadConnected!=null) {

            byte[] bytesToSend = "X".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

} // END



