package infinitedreams.shanjinur.sipo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class InstructionClass extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         setContentView(R.layout.instructions);
    }

    @Override
    public void onBackPressed(){
        InstructionClass.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        startActivity(new Intent(InstructionClass.this,Welcome.class));
    }
}
