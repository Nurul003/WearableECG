package infinitedreams.shanjinur.sipo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.tensorflow.lite.Interpreter;
import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class sensordata extends Activity {
    AtomicBoolean atomicBoolean ;
    String address = null;
    private ProgressDialog progress;
    int arrayLength=698;
    float[] dataArray=new float[arrayLength];
    int counter=0;
    Interpreter tfLite;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds ;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private static final float TOTAL_MEMORY = 16.0f;
    private static final float LIMIT_MAX_MEMORY = 12.0f;
    private LineChart mChart;
    private Thread thread;
    private boolean isBtConnected = false;
    private Button measure;
    private Button start,reset,pause ;
    private Typeface typeface ;
    private Button discon ;
    private TextView stopwatch ,sensorvalue;
    boolean sus ;
    private boolean plotData = true;
    boolean measured ;
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/model.tflite";
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    protected void onCreate(Bundle savedInstanceState) {
        Context context = this ;
        super.onCreate(savedInstanceState);
        handler = new Handler() ;
        setContentView(R.layout.sensordata_layout);
        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            tfLite = new Interpreter(loadModelFile(getAssets(), actualModelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mChart = (LineChart) findViewById(R.id.chart1);

        // enable description text
        mChart.getDescription().setEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.DKGRAY);

        // ********data***********
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // ********add empty data***********
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMaximum(1024f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(true);
        mChart.getXAxis().setDrawGridLines(true);
        mChart.setDrawBorders(true);

        AssetManager am = context.getApplicationContext().getAssets();
        handler = new Handler() ;
        atomicBoolean = new AtomicBoolean(false) ;

        typeface = Typeface.createFromAsset(am, String.format(Locale.US, "fonts/%s", "font.ttf"));
        Intent newint = getIntent();
        address = newint.getStringExtra(devicelist.EXTRA_ADDRESS); //receive the address of the bluetooth device
        measure = findViewById(R.id.measure) ;
        measure.setTypeface(typeface);
        discon = findViewById(R.id.disconnect) ;
        discon.setTypeface(typeface);
        stopwatch = findViewById(R.id.tvTimer) ;
        stopwatch.setTypeface(typeface);
        sensorvalue = findViewById(R.id.sensorvalue) ;
        sensorvalue.setTypeface(typeface);
        start = findViewById(R.id.start) ;
        start.setTypeface(typeface);
        pause = findViewById(R.id.btPause) ;
        pause.setTypeface(typeface);
        reset = findViewById(R.id.btReset) ;
        reset.setTypeface(typeface);
        sus = false ;
        new ConnectBT().execute(); //Call the class to connect
        measured = false ;
        //feedMultiple();
        start.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View view) {
                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);
                reset.setEnabled(false);
                HandlerThread handlerThread = new HandlerThread("") ;
                handlerThread.start();
                Handler h = new Handler(handlerThread.getLooper()) ;
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        while (true){
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            int soun = 0 ;
                            float input=soun;
                            int c=0;
                            if (btSocket!=null)
                            {
                                try
                                {
                                    measured = true ;
                                    btSocket.getOutputStream().write("1".getBytes()) ;
                                    int b1 = btSocket.getInputStream().read() ;
                                    int b2 = btSocket.getInputStream().read() ;
                                    soun = b2 + b1*256 ;
                                    dataArray[counter]=soun;
                                    counter++;
                                    //Toast.makeText(getApplicationContext(),String.valueOf(soun),Toast.LENGTH_SHORT).show();
                                    System.out.println("ji"+soun);
                                    addEntry(soun);
                                    if(counter==(arrayLength-1)){
                                        classifier(dataArray);
                                        counter=0;
                                    }
                                    //classify()
                                    final int finalSoun = soun ;
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            sensorvalue.setText(String.valueOf(finalSoun));
//                                        }
//                                    });
                                }
                                catch (IOException e)
                                {
                                    msg("Error");
                                }
                            }
                        }
                    }
                },0) ;
            }
        });
    }

    private void addEntry(int event) {

        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

//            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);
            data.addEntry(new Entry(set.getEntryCount(), event), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(30);
             //mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());

        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(1f);
        set.setColor(Color.RED);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }
    private void feedMultiple() {

        if (thread != null){
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true){
                    plotData = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }
    private void classifier(float[] test){
        float[][][] sensorData=new float[1][arrayLength][1];
        for(int i=0;i<698;i++)
            sensorData[0][i][0]=test[i];
        float[][] yPredict=new float[1][14];
        tfLite.run(sensorData,yPredict);
        for(int j=0;j<14;j++)
            System.out.println(yPredict[0][j]);
    }
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void getSensorData(View view)
    {
        int soun = 0 ;
        if (btSocket!=null)
        {
            try
            {
                measured = true ;
                btSocket.getOutputStream().write("1".getBytes()) ;
                int b1 = btSocket.getInputStream().read() ;
                //System.out.println(b1);
                int b2 = btSocket.getInputStream().read() ;
                System.out.println(b2+"HI");
                soun = b2 + b1*256 ;
                //System.out.println(soun);
                sensorvalue.setText(String.valueOf(soun)) ;
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    public void msg(String s)
    {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void reset(View view) {
        MillisecondTime = 0L ;
        StartTime = 0L ;
        TimeBuff = 0L ;
        UpdateTime = 0L ;
        Seconds = 0 ;
        Minutes = 0 ;
        MilliSeconds = 0 ;
        measured = false ;
        stopwatch.setText("00:00:00");
        sensorvalue.setText("");
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout
    }

    public void setPause(View view) {
        TimeBuff += MillisecondTime;
        handler.removeCallbacks(runnable);
        reset.setEnabled(true);
    }

    public void disconnect(View view) {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            stopwatch.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(sensordata.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        private void msg(String s)
        {
            Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}