package poc.wachi.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.ufro.android.mozbiisdk.MozbiiBleCallBack;
import com.ufro.android.mozbiisdk.MozbiiBleWrapper;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements MozbiiBleCallBack{
    private MozbiiBleWrapper wrapper;
    private TextView status;
    private TextView text;
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView)findViewById(R.id.status);
        text = (TextView)findViewById(R.id.text);

        wrapper = new MozbiiBleWrapper(this, this);
        wrapper.initialize();
        if(!wrapper.isBtEnabled()){
            // do log
        }

        wrapper.startScanning();
    }

    @Override
    public void onMozbiiConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText("Connected");
            }
        });
        wrapper.stopScanning();
    }

    @Override
    public void onMozbiiDisconnected() {
    }

    @Override
    public void onMozbiiColorIndexChanged(int index) {
        this.index = index;
    }

    @Override
    public void onMozbiiColorArrayChanged(final int[] colors) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setTextColor(colors[index]);
            }
        });
    }

    @Override
    public void onMozbiiBatteryStatusChanged(int battery) {

    }
}
