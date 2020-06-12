package infinitedreams.shanjinur.sipo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class Welcome extends Activity {

    private TextView instructions ;
    private TextView connarduino ;
    private TextView sipo ;
    private TextView welcome ;
    private Typeface typeface ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = this ;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        instructions = findViewById(R.id.instructions) ;
        welcome = findViewById(R.id.welcome) ;
        sipo = findViewById(R.id.sipo) ;
        connarduino = findViewById(R.id.connarduino) ;
        AssetManager am = context.getApplicationContext().getAssets();


        typeface = Typeface.createFromAsset(am,
                String.format(Locale.US, "fonts/%s", "font.ttf"));
        welcome.setTypeface(typeface);
        sipo.setTypeface(typeface);
        instructions.setTypeface(typeface);
        connarduino.setTypeface(typeface);
        instructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Welcome.this,InstructionClass.class));
                Welcome.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        connarduino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Welcome.this,devicelist.class));
                Welcome.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });
    }

    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }
}
