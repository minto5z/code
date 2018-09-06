package ws.design.com.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ws.design.com.R;
import ws.design.com.data.model.Graph;
import ws.design.com.data.model.Point;
import ws.design.com.data.model.SOAnswersResponse;
import ws.design.com.data.remote.ApiUtils;
import ws.design.com.data.remote.SOService;

public class MainActivity extends AppCompatActivity {

    private LineChart mChart,mChart1;
    private Uri mImageCaptureUri;
    ImageView banar1;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int PICK_FROM_FILE = 3;
    private static String TAG = MainActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;

    private List<Graph> mItems;
    private List<Point> mPoints;
    private SOService mService;

    private List<Entry> xValue = new ArrayList<>();
    private List<Entry> yValue = new ArrayList<>();
    private List<Entry> zValue = new ArrayList<>();
    private List<Entry> aThreshold = new ArrayList<>();
    private List<Entry> wThreshold = new ArrayList<>();
    private  List<String> timestream = new ArrayList<>();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT, Gravity.END);
        String[] web = MainActivity.this.getResources().getStringArray(R.array.nav_drawer_labels);
        Integer[] imageId = {
                R.drawable.ic_home,
                R.drawable.ic_tv,
                R.drawable.ic_ticket,
                R.drawable.ic_offers,
                R.drawable.ic_earning,
                R.drawable.ic_usemoney,
                R.drawable.ic_ranking,
                R.drawable.ic_faq,
                R.drawable.ic_symbol21,
        };
        CustomList adapter1 = new CustomList(MainActivity.this, web, imageId);
        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(adapter1);
        final String[] items = new String[]{"Take from camera", "Select from gallery"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, items);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) { //pick from camera
                if (item == 0) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                            "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                    try {
                        intent.putExtra("return-data", true);
                        startActivityForResult(intent, PICK_FROM_CAMERA);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                } else { //pick from file
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE);
                }
            }
        });
        final AlertDialog dialog = builder.create();
        banar1 = (ImageView) findViewById(R.id.banar1);
        banar1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        //mToolbar.setTitle("Monitoring Home");


        //mToolbar.addView(new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        //mToolbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);


//--------------------------------------------------------------------------------------start graph code -------------------------------------------------
        mChart = (LineChart) findViewById(R.id.chart);
        mChart1 = (LineChart) findViewById(R.id.chart1);
        mChart.setBackgroundColor(Color.rgb(31, 54, 72));
        mChart1.setBackgroundColor(Color.rgb(31, 54, 72));

        mService = ApiUtils.getSOService();
        progressDialog= new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading data...");
        progressDialog.show();
        loadAnswers();

//--------------------------------------------------------------------------------------end graph code------------------------------------------------
    }

    public void loadAnswers() {
        mService.getAnswers().enqueue(new Callback<SOAnswersResponse>() {
            @Override
            public void onResponse(Call<SOAnswersResponse> call, Response<SOAnswersResponse> response) {

                if (response.isSuccessful()) {
                    mItems = response.body().getItems();
                    for (int j=0;j<mItems.size();j++) {
                        if (xValue != null && !xValue.isEmpty()&&yValue != null && !yValue.isEmpty()&&zValue != null && !zValue.isEmpty()) {
                            xValue.clear();yValue.clear();zValue.clear();
                        }
                        String graphName =mItems.get(j).getName();
                        Integer alertThreshold = mItems.get(j).getAlert_threshold();
                        Float warningThreshold = mItems.get(j).getWarning_threshold();
                        mPoints = mItems.get(j).getPoints();
                        for (int i = 0; i < mPoints.size(); i++) {
                            xValue.add(new Entry(i,mPoints.get(i).getAxis_value().getX()));
                            yValue.add(new Entry(i,mPoints.get(i).getAxis_value().getY()));
                            zValue.add(new Entry(i,mPoints.get(i).getAxis_value().getZ()));
                            aThreshold.add(new Entry(i,alertThreshold));
                            wThreshold.add(new Entry(i,warningThreshold));
                            timestream.add(mPoints.get(i).getTimestamp().toString());
                        }
                        //Log.d("AnswersPresenter", "---------------------" + mPoints.get(j).getAxis_value().getX() + "---------------------");
                        if(j==0)setData(xValue,yValue,zValue,graphName,aThreshold,wThreshold,timestream,mPoints.size());
                        else setData1(xValue,yValue,zValue,graphName,aThreshold,wThreshold,timestream,mPoints.size());
                    }
                    progressDialog.dismiss();

                } else {
                    int statusCode = response.code();
                    Log.d("AnswersPresenter", "----------------------handle request errors" + statusCode + "---------------------");
                }
            }

            @Override
            public void onFailure(Call<SOAnswersResponse> call, Throwable t) {
                //showErrorMessage();
                Log.d("AnswersPresenter", "error loading from API");

            }
        });
    }

    private void setData1(List<Entry> xValue, List<Entry> yValue, List<Entry> zValue,String name,List<Entry> aThreshold,List<Entry> wThreshold,List<String> timestream,int pointSize) {
        LineDataSet set1 = new LineDataSet(xValue, "X Value");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(1.5f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        //set1.setDrawFilled(true);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);

        LineDataSet set2 = new LineDataSet(yValue, "Y Value");
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);
        set2.setColor(Color.GREEN);
        set2.setValueTextColor(Color.GREEN);
        set2.setLineWidth(1.5f);
        set2.setDrawCircles(false);
        set2.setDrawValues(false);
        //set2.setDrawFilled(true);
        set2.setFillAlpha(65);
        set2.setFillColor(Color.GREEN);
        set2.setHighLightColor(Color.GREEN);
        set2.setDrawCircleHole(false);

        LineDataSet set3 = new LineDataSet(zValue, "Z Value");
        set3.setAxisDependency(YAxis.AxisDependency.LEFT);
        set3.setColor(Color.MAGENTA);
        set3.setValueTextColor(Color.MAGENTA);
        set3.setLineWidth(1.5f);
        set3.setDrawCircles(false);
        set3.setDrawValues(false);
        //set3.setDrawFilled(true);
        set3.setFillAlpha(65);
        set3.setFillColor(Color.MAGENTA);
        set3.setHighLightColor(Color.MAGENTA);
        set3.setDrawCircleHole(false);

        LineDataSet set4 = new LineDataSet(aThreshold, "Alart");
        set4.setAxisDependency(YAxis.AxisDependency.LEFT);
        set4.setColor(Color.RED);
        set4.setValueTextColor(Color.RED);
        set4.setLineWidth(1.5f);
        set4.setDrawCircles(false);
        set4.setDrawValues(false);
        //set3.setDrawFilled(true);
        set4.setFillAlpha(65);
        set4.setFillColor(Color.RED);
        set4.setHighLightColor(Color.RED);
        set4.setDrawCircleHole(false);



        LineDataSet set5 = new LineDataSet(wThreshold, "Warning");
        set5.setAxisDependency(YAxis.AxisDependency.LEFT);
        set5.setColor(Color.YELLOW);
        set5.setValueTextColor(Color.WHITE);
        set5.setLineWidth(1.5f);
        set5.setDrawCircles(false);
        set5.setDrawValues(false);
        //set3.setDrawFilled(true);
        set5.setFillAlpha(65);
        set5.setFillColor(Color.WHITE);
        set5.setHighLightColor(Color.WHITE);
        set5.setDrawCircleHole(false);

        LineData data = new LineData(set1,set2,set3,set4,set5);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        mChart1.getAxisRight().setEnabled(false);
        // set data
        mChart1.setData(data);
        mChart1.setVisibleXRangeMaximum(50);
        mChart1.getDescription().setText(name);
        mChart1.getDescription().setTextColor(Color.WHITE);
        //mChart.getDescription().setPosition(0f,2.5f);
        mChart1.moveViewToX(data.getEntryCount());


        Legend l = mChart1.getLegend();
        l.setEnabled(true);

        XAxis xAxis = mChart1.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setCenterAxisLabels(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new MyXAxisValueFormatter(timestream));

        YAxis y = mChart1.getAxisLeft();
        //y.setLabelCount(6, false);
        y.setTextColor(Color.WHITE);
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setDrawGridLines(false);
        y.setAxisLineColor(Color.WHITE);
        YAxis.AxisDependency axisDependency = null;
        mChart1.moveViewToAnimated(pointSize, pointSize, axisDependency, 10000);

    }

    public class MyXAxisValueFormatter implements IAxisValueFormatter {

        private List<String> mValues;

        public MyXAxisValueFormatter(List<String> values) {
            this.mValues = values;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            String date = getDateCurrentTimeZone(Integer.parseInt(mValues.get((int) value)));
            return date;
        }
    }
    public  String getDateCurrentTimeZone(int timestamp) {
        try{
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(timestamp * 1000);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date currenTimeZone = (Date) calendar.getTime();
            return sdf.format(currenTimeZone);
        }catch (Exception e) {
        }
        return "";
    }
    private void setData(List<Entry> xValue, List<Entry> yValue, List<Entry> zValue,String name,List<Entry> aThreshold,List<Entry> wThreshold,List<String> timestream,int pointSize) {

        LineDataSet set1 = new LineDataSet(xValue, "X Value");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(1.5f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        //set1.setDrawFilled(true);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);

        LineDataSet set2 = new LineDataSet(yValue, "Y Value");
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);
        set2.setColor(Color.GREEN);
        set2.setValueTextColor(Color.GREEN);
        set2.setLineWidth(1.5f);
        set2.setDrawCircles(false);
        set2.setDrawValues(false);
        //set2.setDrawFilled(true);
        set2.setFillAlpha(65);
        set2.setFillColor(Color.GREEN);
        set2.setHighLightColor(Color.GREEN);
        set2.setDrawCircleHole(false);

        LineDataSet set3 = new LineDataSet(zValue, "Z Value");
        set3.setAxisDependency(YAxis.AxisDependency.LEFT);
        set3.setColor(Color.MAGENTA);
        set3.setValueTextColor(Color.MAGENTA);
        set3.setLineWidth(1.5f);
        set3.setDrawCircles(false);
        set3.setDrawValues(false);
        //set3.setDrawFilled(true);
        set3.setFillAlpha(65);
        set3.setFillColor(Color.MAGENTA);
        set3.setHighLightColor(Color.MAGENTA);
        set3.setDrawCircleHole(false);
        LineDataSet set4 = new LineDataSet(aThreshold, "Alart");
        set4.setAxisDependency(YAxis.AxisDependency.LEFT);
        set4.setColor(Color.RED);
        set4.setValueTextColor(Color.RED);
        set4.setLineWidth(1.5f);
        set4.setDrawCircles(false);
        set4.setDrawValues(false);
        //set3.setDrawFilled(true);
        set4.setFillAlpha(65);
        set4.setFillColor(Color.RED);
        set4.setHighLightColor(Color.RED);
        set4.setDrawCircleHole(false);



        LineDataSet set5 = new LineDataSet(wThreshold, "Warning");
        set5.setAxisDependency(YAxis.AxisDependency.LEFT);
        set5.setColor(Color.YELLOW);
        set5.setValueTextColor(Color.YELLOW);
        set5.setLineWidth(1.5f);
        set5.setDrawCircles(false);
        set5.setDrawValues(false);
        //set3.setDrawFilled(true);
        set5.setFillAlpha(65);
        set5.setFillColor(Color.YELLOW);
        set5.setHighLightColor(Color.YELLOW);
        set5.setDrawCircleHole(false);

        LineData data = new LineData(set1,set2,set3,set4,set5);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        mChart.getAxisRight().setEnabled(false);
        // set data
        mChart.setData(data);
        mChart.setVisibleXRangeMaximum(50);
        mChart.getDescription().setText(name);
        mChart.getDescription().setTextColor(Color.WHITE);
        //mChart.moveViewToX(data.getEntryCount());


        Legend l = mChart.getLegend();
        l.setEnabled(true);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setCenterAxisLabels(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new MyXAxisValueFormatter(timestream));

        YAxis y = mChart.getAxisLeft();
        //y.setLabelCount(6, false);
        y.setTextColor(Color.WHITE);
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setDrawGridLines(false);
        y.setAxisLineColor(Color.WHITE);
        YAxis.AxisDependency axisDependency = null;
        mChart.moveViewToAnimated(pointSize, pointSize, axisDependency, 10000);
    }
    public void showErrorMessage() {
        Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case PICK_FROM_CAMERA:
                doCrop();

                break;

            case PICK_FROM_FILE:
                mImageCaptureUri = data.getData();

                doCrop();

                break;

            case CROP_FROM_CAMERA:
                Bundle extras = data.getExtras();

                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");

                    banar1.setImageBitmap(photo);
                }

                File f = new File(mImageCaptureUri.getPath());

                if (f.exists()) f.delete();

                break;

        }
    }

    private void doCrop() {
        final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);

        int size = list.size();

        if (size == 0) {
            Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();

            return;
        } else {
            intent.setData(mImageCaptureUri);

            intent.putExtra("outputX", 200);
            intent.putExtra("outputY", 200);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);

            if (size == 1) {
                Intent i = new Intent(intent);
                ResolveInfo res = list.get(0);

                i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

                startActivityForResult(i, CROP_FROM_CAMERA);
            } else {
                for (ResolveInfo res : list) {
                    final CropOption co = new CropOption();

                    co.title = getPackageManager().getApplicationLabel(res.activityInfo.applicationInfo);
                    co.icon = getPackageManager().getApplicationIcon(res.activityInfo.applicationInfo);
                    co.appIntent = new Intent(intent);

                    co.appIntent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

                    cropOptions.add(co);
                }

                CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(), cropOptions);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Crop App");
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        startActivityForResult(cropOptions.get(item).appIntent, CROP_FROM_CAMERA);
                    }
                });

                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                        if (mImageCaptureUri != null) {
                            getContentResolver().delete(mImageCaptureUri, null, null);
                            mImageCaptureUri = null;
                        }
                    }
                });

                AlertDialog alert = builder.create();

                alert.show();
            }
        }
    }

    public class CustomList extends ArrayAdapter<String> {

        private final Activity context;
        private final String[] web;
        private final Integer[] imageId;

        public CustomList(Activity context,
                          String[] web, Integer[] imageId) {
            super(context, R.layout.nav_drawer_row, web);
            this.context = context;
            this.web = web;
            this.imageId = imageId;

        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();


            View rowView = inflater.inflate(R.layout.nav_drawer_row, null, true);
            TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);


            ImageView imageView = (ImageView) rowView.findViewById(R.id.img);
            txtTitle.setText(web[position]);

            imageView.setImageResource(imageId[position]);
            return rowView;
        }
    }
    /*@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }*/
}