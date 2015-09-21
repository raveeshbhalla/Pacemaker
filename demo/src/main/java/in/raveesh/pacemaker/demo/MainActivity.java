package in.raveesh.pacemaker.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import static in.raveesh.pacemaker.Pacemaker.cancelLinear;
import static in.raveesh.pacemaker.Pacemaker.scheduleLinear;

public class MainActivity extends Activity {

    boolean beginOrStop = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button set = (Button)findViewById(R.id.begin);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!beginOrStop) {
                    scheduleLinear(MainActivity.this, 5);
                    set.setText(R.string.stop);
                }
                else{
                    cancelLinear(MainActivity.this, 5);
                    set.setText(R.string.begin);
                }
                beginOrStop = !beginOrStop;
            }
        });
    }
}
