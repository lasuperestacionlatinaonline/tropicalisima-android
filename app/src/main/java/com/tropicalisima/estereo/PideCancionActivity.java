package com.tropicalisima.estereo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** El oyente confirma el envío en WhatsApp: nunca se envía automáticamente. */
public final class PideCancionActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);draw();}
    private EditText entry(String hint){
        EditText e=new EditText(this);e.setTextSize(17);e.setTextColor(Ui.WHITE);
        e.setHintTextColor(0xff9da9bd);e.setHint(hint);
        e.setPadding(Ui.dp(this,15),Ui.dp(this,14),Ui.dp(this,15),Ui.dp(this,14));
        e.setBackground(Ui.solid(this,0xff0e122b,15,0xff56507a));
        e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setSingleLine(true);return e;
    }
    private void draw(){
        FrameLayout root=Ui.frame(this,Ui.PINK);setContentView(root);
        LinearLayout col=Ui.scrollContent(this,root);
        TextView back=Ui.text(this,"‹  VOLVER A LA RADIO",16,Ui.CYAN,true);
        back.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,14));back.setOnClickListener(v->finish());Ui.add(col,back,2);
        Ui.add(col,Ui.heading(this,"PIDE TU CANCIÓN"),16);
        Ui.add(col,Ui.subtitle(this,"¡La música nos conecta!"),4);
        LinearLayout panel=Ui.card(this);Ui.add(col,panel,22);
        Ui.add(panel,Ui.text(this,"TU NOMBRE",16,Ui.GOLD,true),0);
        EditText name=entry("¿Cómo te llamas?");Ui.add(panel,name,8);
        Ui.add(panel,Ui.text(this,"ARTISTA O CANCIÓN",16,Ui.GOLD,true),20);
        EditText song=entry("¿Qué quieres escuchar?");Ui.add(panel,song,8);
        Ui.add(panel,Ui.text(this,"DEDICATORIA (OPCIONAL)",16,Ui.GOLD,true),20);
        EditText dedication=entry("¿Para quién es tu saludo?");Ui.add(panel,dedication,8);
        TextView notice=Ui.text(this,"",15,Ui.ORANGE,false);Ui.add(panel,notice,8);
        Ui.add(panel,Ui.action(this,"ENVIAR PEDIDO POR WHATSAPP  ↗",Ui.PINK,Ui.ORANGE,()->{
            String n=name.getText().toString().trim(),s=song.getText().toString().trim(),d=dedication.getText().toString().trim();
            if(n.isEmpty()||s.isEmpty()){notice.setText("Escribe tu nombre y la canción que deseas.");return;}
            if(n.length()>70||s.length()>140||d.length()>230){notice.setText("Revisa la longitud de los textos.");return;}
            String text="¡Hola, TROPICALISIMA ESTÉREO! 🎶\nSoy: "+n+"\nPido la canción: "+s+(d.isEmpty()?"":"\nDedicatoria: "+d)+"\n¡La que te pone a gozar!";
            String encoded=Uri.encode(text);
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/573011835350?text="+encoded)));}
            catch(Exception e){Toast.makeText(this,"No se pudo abrir WhatsApp o el navegador.",Toast.LENGTH_LONG).show();}
        }),20);
        Ui.add(panel,Ui.text(this,"WhatsApp se abrirá con el texto preparado. Tú decides cuándo enviarlo.",14,Ui.MUTE,false),11);
    }
}
