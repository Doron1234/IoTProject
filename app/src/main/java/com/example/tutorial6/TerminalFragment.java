package com.example.tutorial6;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    LineChart mpLineChart;
    LineDataSet lineDataSetN;
    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineData data;

    private String fileName;
    private boolean start_flag = false;
    private long startTime;
    private boolean save_flag = false;
    private TextView fileOpenText;
    Button newGuy;

    String selectedInSpinner = "Walking";
    TextView textViewEstimated;

    int sum = 0;
    float before = 0, current = 0, after = 0;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");

    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }


    private void updateLine(String toUpdate, String updated) throws IOException {
        File f = new File("/sdcard/csv_dir/" + fileName + ".csv");
        BufferedReader file = new BufferedReader(new FileReader(f));
        String line;
        String input = "";

        while ((line = file.readLine()) != null)
            input += line + System.lineSeparator();

        input = input.replace(toUpdate, updated);

        FileOutputStream os = new FileOutputStream(f);
        os.write(input.getBytes());

        file.close();
        os.close();
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        newGuy = view.findViewById(R.id.button);
        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));

        mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
        lineDataSetN =  new LineDataSet(emptyDataValues(), "N");
        lineDataSetN.setColor(Color.BLUE);
        lineDataSetN.setCircleColor(Color.BLUE);
        dataSets.add(lineDataSetN);
        data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();

        Button buttonCsvShow = (Button) view.findViewById(R.id.button2);

        Button buttonStart = (Button) view.findViewById(R.id.start_button);
        Button buttonStop = (Button) view.findViewById(R.id.stop_button);
        Button buttonReset = (Button) view.findViewById(R.id.reset_button);
        Button buttonSave = (Button) view.findViewById(R.id.save_button);
        Spinner activity_spinner = (Spinner) view.findViewById(R.id.spinner_choose_activity);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.activity_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activity_spinner.setAdapter(adapter);
        TextView editTextSteps = (TextView) view.findViewById(R.id.editTextSteps);
        TextView editTextFileName = (TextView) view.findViewById(R.id.editTextFileName);
        textViewEstimated = (TextView) view.findViewById(R.id.textViewEstimated);

        activity_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedInSpinner = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No item is selected
            }
        });

        newGuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(),MapDemoActivity.class);
                //intent.putExtra("fileOpenText", fileOpenText.getText().toString());
                startActivity(intent);
            }
        });


        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {

                    sum = 0;
                    // getting number of steps and file name
                    int steps = Integer.parseInt(editTextSteps.getText().toString());
                    fileName = editTextFileName.getText().toString();

                    // create new csv unless file already exists
                    File file = new File("/sdcard/csv_dir/");
                    file.mkdirs();
                    String csv = "/sdcard/csv_dir/" + fileName + ".csv";
                    CSVWriter csvWriter = new CSVWriter(new FileWriter(csv,true));

                    // parse string values, in this case [0] is tmp & [1] is count (t)
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    Date date = new Date();

                    String row0[]= new String[]{fileName, "NAME:"};
                    String row1[] = new String[]{formatter.format(date), "EXPERIMENT TIME:"};
                    String row2[] = new String[]{selectedInSpinner, "ACTIVITY TYPE:"};
                    String row3[] = new String[]{String.valueOf(steps), "COUNT OF ACTUAL STEPS"};
                    String row4[] = new String[]{"Two roads diverge in a yellow wood"};
                    String row5[] = new String[]{"ACC Z", "ACC Y", "ACC X", "Time [sec]"};

                    csvWriter.writeNext(row0);
                    csvWriter.writeNext(row1);
                    csvWriter.writeNext(row2);
                    csvWriter.writeNext(row3);
                    csvWriter.writeNext(row4);
                    csvWriter.writeNext(row5);

                    startTime = System.currentTimeMillis();
                    start_flag = true;



                    csvWriter.close();




                } catch (IOException e) {
                    e.printStackTrace();
                }}
        });

        buttonCsvShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenLoadCSV();

            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                start_flag = false;
                String s = "Two roads diverge in a yellow wood";
                try {
                    updateLine(s,  "Estimated number of steps\",\"" + textViewEstimated.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        buttonSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getContext(),"Clear",Toast.LENGTH_SHORT).show();
                mpLineChart.clearValues();
                mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
                lineDataSetN =  new LineDataSet(emptyDataValues(), "N");
                lineDataSetN.setColor(Color.BLUE);
                lineDataSetN.setCircleColor(Color.BLUE);
                dataSets.add(lineDataSetN);
                data = new LineData(dataSets);
                mpLineChart.setData(data);
                mpLineChart.invalidate();
            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                File f = new File("/sdcard/csv_dir/" + fileName + ".csv");
                f.delete();
                Toast.makeText(getContext(),"Clear",Toast.LENGTH_SHORT).show();
                mpLineChart.clearValues();
                mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
                lineDataSetN =  new LineDataSet(emptyDataValues(), "N");
                lineDataSetN.setColor(Color.BLUE);
                lineDataSetN.setCircleColor(Color.BLUE);
                dataSets.add(lineDataSetN);
                data = new LineData(dataSets);
                mpLineChart.setData(data);
                mpLineChart.invalidate();
            }
        });

        return view;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr){
        for (int i = 0; i < stringsArr.length; i++)  {
            stringsArr[i]=stringsArr[i].replaceAll(" ","");
        }


        return stringsArr;
    }
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] message) {
        if(hexEnabled) {
            receiveText.append(TextUtil.toHexString(message) + '\n');
        } else {
            String msg = new String(message);
            if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                String msg_to_save = msg;
                msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
                // check message length
                if (msg_to_save.length() > 1){
                    // split message string by ',' char
                    String[] parts = msg_to_save.split(",");
                    // function to trim blank spaces
                    parts = clean_str(parts);

                    // saving data to csv
                    try {

                        // create new csv unless file already exists
                        File file = new File("/sdcard/csv_dir/");
                        file.mkdirs();
                        String csv = "/sdcard/csv_dir/" + fileName + ".csv";
                        CSVWriter csvWriter = new CSVWriter(new FileWriter(csv,true));



                        // add received values to line dataset for plotting the linechart
                        long timePassed = System.currentTimeMillis() - startTime;
                        float z = Float.parseFloat(parts[0]);
                        float y = Float.parseFloat(parts[1]);
                        float x = Float.parseFloat(parts[2]);
                        data.addEntry(new Entry(((float)timePassed)/1000,(float)Math.sqrt(x * x + y * y + z * z)),0);
                        lineDataSetN.notifyDataSetChanged(); // let the data know a dataSet changed
                        mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                        mpLineChart.invalidate(); // refresh


                        before = current;
                        current = after;
                        after = (float)Math.sqrt(x * x + y * y + z * z);
                        // parse string values, in this case [0] is tmp & [1] is count (t)
                        String row[]= new String[]{parts[0],parts[1], parts[2], String.valueOf((float)timePassed/1000)};
                        csvWriter.writeNext(row);
                        csvWriter.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }}
                try{
                if(!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getContext()));
                }

                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("notpythoncode");
                System.out.println(pyobj.toString());


                PyObject obj = pyobj.callAttr("m", before, current, after);
                sum += obj.toInt();
                textViewEstimated.setText(String.valueOf(sum));




            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }

            msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
            // send msg to function that saves it to csv
            // special handling if CR and LF come in separate fragments
            if (pendingNewline && msg.charAt(0) == '\n') {
                Editable edt = receiveText.getEditableText();
                if (edt != null && edt.length() > 1)
                    edt.replace(edt.length() - 2, edt.length(), "");
            }
            pendingNewline = msg.charAt(msg.length() - 1) == '\r';
        }
        receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
    }
}

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        try {
            if(start_flag)
                receive(data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues()
    {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }

    private void OpenLoadCSV(){
        Intent intent = new Intent(getContext(),LoadCSV.class);
        //intent.putExtra("fileOpenText", fileOpenText.getText().toString());
        startActivity(intent);
    }

}
