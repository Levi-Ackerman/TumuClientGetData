package com.example.zhendongchuanganqi;

import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.tsz.afinal.FinalActivity;
import net.tsz.afinal.annotation.view.ViewInject;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends FinalActivity implements
		OnCheckedChangeListener {

	private TextView textView;
	private EditText etName;
	// private Button button;
	// private EditText editText;
	private String text;
	// private int rate;
	private SensorManager sensors;
	private Sensor sensor;
	private myListener listener;
	// private FinalDb db;
	@ViewInject(id = R.id.tx_time)
	TextView tv_time;
	@ViewInject(id = R.id.cb_start)
	CheckBox cb_start;

	DataOutputStream dos;
	private Timer timer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textView = (TextView) findViewById(R.id.textView1);
		etName = (EditText)findViewById(R.id.etName);
		// rate = getResources().getInteger(R.integer.rate);
		listener = new myListener();
		cb_start.setOnCheckedChangeListener(this);
		// db = FinalDb.create(this);
		sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensor = sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		time_elems = new ArrayList<>();
		write_elems = new ArrayList<>();
		write_elems50 =new ArrayList<>();
		write_elems100 = new ArrayList<>();
	}

	long startMillin;
	FileWriter fileWriter;
	FileWriter fileWriter100;
	FileWriter fileWriter50;
	private int Frequence1 = 50;
	private int Frequence2 = 100;

	List<Elem> time_elems;
	List<String> write_elems, write_elems100, write_elems50;

	int index = 0;
	double lastX = 0, lastY = 0;
	class Elem {
		public Elem(long milliSec, float acce) {
			this.milliSec = milliSec;
			this.acce = acce;
		}

		public long milliSec;
		public float acce;
	}
	boolean isFirst = true;

	class myListener implements SensorEventListener {

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (isFirst) {
				isFirst = false;
				startMillin = System.currentTimeMillis();
			}else {
				long delay = (System.currentTimeMillis() - startMillin);
				time_elems.add(new Elem(delay, event.values[2]));
			}
			text = "" + event.values[2];
			textView.setText(text);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	}

	class WriteRunnable extends TimerTask{

		@Override
		public void run() {
			int count = time_elems.size();
			if (count == 0)
				return;
			if (index == 0) {
				index++;
				lastX = time_elems.get(0).milliSec;
				lastY = time_elems.get(0).acce;
				time_elems.remove(0);
			} else {
				for (int i = 0; i < count; i++) {
					write_elems.add(time_elems.get(0).milliSec+"\t"+time_elems.get(0).acce+"\n");
					//给100hz的插值
					long sec = time_elems.get(0).milliSec;
					if (sec < 1000/Frequence2 * index) {
						lastX = sec;
						lastY = time_elems.get(0).acce;
					} else if (sec == 1000/Frequence2 * index) {
						lastX = sec;
						lastY = time_elems.get(0).acce;
						if(index++%2==0)
							write_elems50.add(lastX +"\t"+lastY+"\n");
						write_elems100.add(lastX + "\t" + lastY + "\n");
					} else {
						float y = time_elems.get(0).acce;
						lastY = (y - lastY) / (sec - lastX)
								* (1000/Frequence2 * index - lastX) + lastY;
						lastX = 1000/Frequence2 * index;
						if(index++%2==0)
							write_elems50.add(lastX +"\t"+lastY+"\n");
						write_elems100.add(lastX + "\t" + lastY + "\n");
					}
					time_elems.remove(0);
				}
			}
		}
	}

	public String filepath, filepath100, filepath50;

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if (isChecked) {
			isFirst = true;
			String name = etName.getText().toString().trim();
			if(TextUtils.isEmpty(name)){
				filepath = Environment.getExternalStorageDirectory().toString()
						+ "/AccelerateData.txt";
				filepath100 = Environment.getExternalStorageDirectory().toString()
						+ "/AccelerateData.100.txt";
				filepath50 = Environment.getExternalStorageDirectory().toString()
						+ "/AccelerateData.50.txt";
			}else{
				filepath = Environment.getExternalStorageDirectory().toString()
						+ "/"+name+".txt";
				filepath100 = Environment.getExternalStorageDirectory().toString()
						+ "/"+name+".100.txt";
				filepath50 = Environment.getExternalStorageDirectory().toString()
						+ "/"+name+".50.txt";
			}
			boolean isAvailable = sensors.registerListener(listener, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
			if (!isAvailable) {
				new AlertDialog.Builder(this).setMessage("不支持加速度传感器")
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub
										finish();
									}
								});
			} else {
				time_elems.clear();
				write_elems100.clear();
				write_elems.clear();
				write_elems50.clear();
				index = 0;
				lastX = lastY = 0;
				isFirst = true;
				timer = new Timer();
				timer.schedule(new WriteRunnable(), 10, 500);
				startMillin = System.currentTimeMillis();
				Calendar calendar = Calendar.getInstance();
				String tmText = "开始时间:" + calendar.get(Calendar.HOUR_OF_DAY)
						+ ":" + calendar.get(Calendar.MINUTE) + ":"
						+ calendar.get(Calendar.SECOND) + "."
						+ calendar.get(Calendar.MILLISECOND);
				tv_time.setText(tmText);
				try {
					fileWriter = new FileWriter(filepath, false);
					fileWriter.write(tmText+"\n");
					fileWriter100 = new FileWriter(filepath100, false);
					fileWriter100.write(tmText+"\n");
					fileWriter50 = new FileWriter(filepath50, false);
					fileWriter50.write(tmText+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			timer.cancel();
			sensors.unregisterListener(listener);
			try {
				for (String write_elem : write_elems100) {
					try {
						fileWriter100.write(write_elem);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				for (String write_elem : write_elems) {
					try {
						fileWriter.write(write_elem);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				for (String write_elem : write_elems50) {
					try {
						fileWriter50.write(write_elem);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				fileWriter.close();
				fileWriter50.close();
				fileWriter100.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
