package com.example.tutorial6;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import java.util.List;


public class LoadCSV extends AppCompatActivity {

    LineData data;
    String selectedFile;
    ArrayList<String[]> csvData;
    TextView estimatedSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_csv);
        Button BackButton = (Button) findViewById(R.id.button_back);
        Button ShowCsvButton = (Button) findViewById(R.id.button_show_csv);
        LineChart lineChart = (LineChart) findViewById(R.id.line_chart);

        csvData = new ArrayList<>();
        Intent intent = getIntent();

        File folder = new File("/sdcard/csv_dir/");
        File[] files = folder.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();
        for(int i = 0; i < files.length; i++)
        {
            fileNames.add(files[i].getName());
        }
        Spinner fileSpinner = findViewById(R.id.spinner_choose_file);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        fileSpinner.setAdapter(adapter);

        estimatedSteps = (TextView) findViewById(R.id.textView);

        fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedFile = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No item is selected
            }
        });
        //String fileOpenText = intent.getStringExtra("fileOpenText");
        //csvData= CsvRead("/sdcard/csv_dir/" + fileOpenText + ".csv");
        /*LineDataSet lineDataSetX =  new LineDataSet(DataValues(csvData, 2),"X");
        LineDataSet lineDataSetY =  new LineDataSet(DataValues(csvData, 1),"Y");
        LineDataSet lineDataSetZ =  new LineDataSet(DataValues(csvData, 0),"Z");
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        lineDataSetX.setColor(Color.BLUE);
        lineDataSetY.setColor(Color.RED);
        lineDataSetZ.setColor(Color.GREEN);
        lineDataSetX.setCircleColor(Color.BLUE);
        lineDataSetY.setCircleColor(Color.RED);
        lineDataSetZ.setCircleColor(Color.GREEN);
        dataSets.add(lineDataSetX);
        dataSets.add(lineDataSetY);
        dataSets.add(lineDataSetZ);
        data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();*/


        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickBack();
            }
        });


        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickBack();
            }
        });

        // TO DO
        ShowCsvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                csvData = CsvRead("/sdcard/csv_dir/" + selectedFile);
                ArrayList<Entry> datx = DataValues(csvData, 2);
                ArrayList<Entry> daty = DataValues(csvData, 1);
                ArrayList<Entry> datz = DataValues(csvData, 0);
                ArrayList<Entry> datt = DataValues(csvData, 3);
                ArrayList<Entry> datN = new ArrayList<>();
                for(int i = 0; i < datx.size(); i++){
                    datN.add(new Entry(datt.get(i).getY(), (float)Math.sqrt(datx.get(i).getY()*datx.get(i).getY() + daty.get(i).getY()*daty.get(i).getY() + datz.get(i).getY()*datz.get(i).getY())));
                }

                estimatedSteps.setText("Estimated number of steps: " + csvData.get(4)[1].toString());


                // LineDataSets
                LineDataSet lineDataSetN =  new LineDataSet(datN,"N");

                // Setting colors
                lineDataSetN.setColor(Color.MAGENTA);

                // Setting circle colors
                lineDataSetN.setCircleColor(Color.MAGENTA);

                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(lineDataSetN);

                data = new LineData(dataSets);
                lineChart.setData(data);
                lineChart.invalidate();
            }
        });
    }

    private void ClickBack(){
        finish();

    }

    private ArrayList<String[]> CsvRead(String path){
        ArrayList<String[]> CsvData = new ArrayList<>();
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[]nextline;
            while((nextline = reader.readNext())!= null){
                if(nextline != null){
                    CsvData.add(nextline);

                }
            }

        }catch (Exception e){}
        return CsvData;
    }

    private ArrayList<Entry> DataValues(ArrayList<String[]> csvData, int column){
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        for (int i = 6; i < csvData.size(); i++){

            dataVals.add(new Entry(Float.parseFloat(csvData.get(i)[3]),
                    Float.parseFloat(csvData.get(i)[column])));
        }
        return dataVals;
    }

}