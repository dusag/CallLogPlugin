package cz.raynet.raynetcrm.calllog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CallDialog {

    public static Dialog create(Context context, String callNumber, boolean incoming) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.calllogdialog);

        final Button saveCall = (Button) dialog.findViewById(R.id.saveCall);
        final Button cancel = (Button) dialog.findViewById(R.id.cancel);
        final TextView callInfo = (TextView) dialog.findViewById(R.id.callInfo);

        final StringBuilder builder = new StringBuilder();
        builder.append("Chcete zapsat ")
                .append(incoming ? "příchozí" : "odchozí")
                .append(" hovor ")
                .append(incoming ? "z čísla " : "na číslo ")
                .append(callNumber)
                .append(" do RAYNET CRM?");

        callInfo.setText(builder.toString());

        saveCall.setEnabled(true);
        cancel.setEnabled(true);

        saveCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Saved to RAYNET...", Toast.LENGTH_LONG).show();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        return dialog;
    }
}
