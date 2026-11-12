package com.tropicalisima.estereo;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Muro nativo: utiliza ÚNICAMENTE la API pública; ninguna contraseña en APK. */
public final class VozActivity extends Activity {
    private static final String API="https://tropicalisimaonline.vercel.app/api/oyentes";
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LinearLayout holder;private TextView tabWrite,tabRead;
    private boolean read=false;
    @Override public void onCreate(Bundle b){super.onCreate(b);draw();}
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
    private void draw(){
        FrameLayout root=Ui.frame(this,Ui.CYAN);setContentView(root);
        LinearLayout col=Ui.scrollContent(this,root);
        TextView back=Ui.text(this,"‹  VOLVER A LA RADIO",16,Ui.CYAN,true);
        back.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,14));back.setOnClickListener(v->finish());Ui.add(col,back,2);
        TextView title=Ui.heading(this,"TU VOZ EN LA RADIO");Ui.add(col,title,12);
        Ui.add(col,Ui.subtitle(this,"¡Déjanos tu saludo, reseña o dedicatoria!"),4);
        LinearLayout tabs=Ui.row(this);Ui.add(col,tabs,22);
        tabWrite=tab("ESCRIBIR",!read,()->setTab(false));
        tabRead=tab("LEER MENSAJES",read,()->setTab(true));
        tabs.addView(tabWrite,new LinearLayout.LayoutParams(0,Ui.dp(this,57),1));
        tabs.addView(tabRead,new LinearLayout.LayoutParams(0,Ui.dp(this,57),1));
        holder=Ui.column(this);Ui.add(col,holder,10);setTab(read);
    }
    private TextView tab(String s,boolean selected,Runnable action){
        TextView t=Ui.text(this,s,16,selected?Ui.GOLD:Ui.MUTE,true);t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.solid(this,selected?0xff3d2147:0xff19182f,15,selected?Ui.PINK:0xff3f3868));
        t.setOnClickListener(v->action.run());return t;
    }
    private void setTab(boolean show){
        read=show;
        tabWrite.setTextColor(show?Ui.MUTE:Ui.GOLD);tabRead.setTextColor(show?Ui.GOLD:Ui.MUTE);
        tabWrite.setBackground(Ui.solid(this,show?0xff19182f:0xff3d2147,15,show?0xff40375a:Ui.PINK));
        tabRead.setBackground(Ui.solid(this,show?0xff3d2147:0xff19182f,15,show?Ui.PINK:0xff40375a));
        holder.removeAllViews();if(read)showList();else showForm();
    }
    private EditText input(String hint,boolean multi){
        EditText e=new EditText(this);e.setTextSize(17);e.setTextColor(Ui.WHITE);e.setHintTextColor(0xff9ca9bf);
        e.setHint(hint);e.setPadding(Ui.dp(this,15),Ui.dp(this,13),Ui.dp(this,15),Ui.dp(this,13));
        e.setBackground(Ui.solid(this,0xff0e122b,15,0xff56507a));
        e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|(multi?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));
        if(multi){e.setMinLines(4);e.setGravity(Gravity.TOP);e.setMaxLines(8);}else e.setSingleLine(true);
        return e;
    }
    private void fieldLabel(LinearLayout panel,String name,int top){Ui.add(panel,Ui.text(this,name,15,Ui.GOLD,true),top);}
    private void showForm(){
        LinearLayout panel=Ui.card(this);Ui.add(holder,panel,0);
        fieldLabel(panel,"TU NOMBRE",1);EditText name=input("¿Cómo te llamas?",false);Ui.add(panel,name,7);
        fieldLabel(panel,"¿QUÉ QUIERES COMPARTIR?",17);
        Spinner type=new Spinner(this);
        type.setBackground(Ui.solid(this,0xff11162f,15,0xff555078));type.setPadding(Ui.dp(this,10),8,Ui.dp(this,10),8);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,
                new String[]{"UN SALUDO","UNA RESEÑA","UNA DEDICATORIA"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);type.setAdapter(adapter);
        Ui.add(panel,type,8);
        fieldLabel(panel,"TU MENSAJE",17);
        EditText message=input("Escribe aquí tu mensaje…",true);Ui.add(panel,message,8);
        TextView notice=Ui.text(this,"",15,Ui.CYAN,false);Ui.add(panel,notice,10);
        final TextView[] submitRef=new TextView[1];
        TextView submit=Ui.action(this,"ENVIAR PARA APROBACIÓN  ↗",Ui.ORANGE,Ui.PINK,()->{
            String n=name.getText().toString().trim(),m=message.getText().toString().trim();
            if(n.length()<2||n.length()>70||m.length()<8||m.length()>400){
                notice.setTextColor(Ui.ORANGE);notice.setText("Escribe tu nombre y un mensaje de entre 8 y 400 caracteres.");return;
            }
            if(m.contains("<")||m.contains(">")||n.contains("<")||n.contains(">")){
                notice.setTextColor(Ui.ORANGE);notice.setText("No se permiten etiquetas.");return;
            }
            String[] values={"saludo","resena","dedicatoria"};
            JSONObject payload=new JSONObject();
            try{payload.put("nombre",n);payload.put("mensaje",m);payload.put("tipo",values[type.getSelectedItemPosition()]);payload.put("sitio_web","");}
            catch(Exception ignored){}
            notice.setTextColor(Ui.MUTE);notice.setText("Enviando mensaje…");
            submitRef[0].setEnabled(false);submitRef[0].setAlpha(.55f);
            io.execute(()->{
                try{
                    JSONObject result=call("POST",payload);if(!result.optBoolean("pendiente",false))throw new Exception("El servidor no confirmó la recepción.");
                    runOnUiThread(()->{if(isFinishing()||isDestroyed())return;
                        name.setText("");message.setText("");type.setSelection(0);
                        notice.setTextColor(Ui.CYAN);notice.setText("¡Mensaje recibido! Está pendiente de aprobación.");
                    });
                }catch(Exception ex){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){
                    notice.setTextColor(Ui.ORANGE);notice.setText(ex.getMessage());}});}
                finally{runOnUiThread(()->{if(!isDestroyed()){submitRef[0].setEnabled(true);submitRef[0].setAlpha(1f);}});}
            });
        });
        submitRef[0]=submit;
        Ui.add(panel,submit,4);
        Ui.add(panel,Ui.text(this,"El saludo se mostrará solamente después de ser aprobado. No incluyas información privada.",13,Ui.MUTE,false),12);
    }
    private void showList(){
        LinearLayout panel=Ui.card(this);Ui.add(holder,panel,0);
        TextView top=Ui.text(this,"SALUDOS DE NUESTRA GENTE",19,Ui.GOLD,true);Ui.add(panel,top,0);
        TextView refresh=Ui.action(this,"↻  ACTUALIZAR MENSAJES",0xff0b7c9b,0xff185dc8,()->showListFresh());
        Ui.add(panel,refresh,10);
        TextView loading=Ui.text(this,"Cargando mensajes aprobados…",15,Ui.MUTE,false);Ui.add(panel,loading,15);
        load(panel,loading);
    }
    private void showListFresh(){holder.removeAllViews();showList();}
    private void load(LinearLayout panel,TextView loading){
        io.execute(()->{
            try{JSONObject result=call("GET",null);JSONArray arr=result.optJSONArray("mensajes");
                if(arr==null)arr=new JSONArray();final JSONArray list=arr;
                runOnUiThread(()->{if(isFinishing()||isDestroyed()||!read)return;panel.removeView(loading);
                    if(list.length()==0){Ui.add(panel,Ui.text(this,"Aún no hay mensajes aprobados. ¡Sé el primero en escribir!",16,Ui.MUTE,false),15);return;}
                    for(int i=0;i<list.length();i++)try{
                        JSONObject item=list.getJSONObject(i);
                        LinearLayout card=Ui.card(this);Ui.add(panel,card,10);
                        Ui.add(card,Ui.text(this,item.optString("nombre","OYENTE"),17,Ui.CYAN,true),0);
                        String tipo=item.optString("tipo","saludo").toUpperCase(java.util.Locale.ROOT);
                        Ui.add(card,Ui.text(this,tipo,12,Ui.GOLD,true),3);
                        Ui.add(card,Ui.text(this,item.optString("mensaje",""),16,Ui.WHITE,false),10);
                    }catch(Exception ignored){}
                });
            }catch(Exception ex){runOnUiThread(()->{if(isFinishing()||isDestroyed()||!read)return;
                loading.setTextColor(Ui.ORANGE);loading.setText(ex.getMessage());});}
        });
    }
    private static JSONObject call(String method,JSONObject payload) throws Exception{
        HttpURLConnection conn=(HttpURLConnection)new URL(API).openConnection();
        try{
            conn.setRequestMethod(method);conn.setConnectTimeout(13000);conn.setReadTimeout(13000);
            conn.setRequestProperty("Accept","application/json");conn.setRequestProperty("Cache-Control","no-cache");conn.setUseCaches(false);
            if(payload!=null){conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
                try(OutputStream out=conn.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}}
            int status=conn.getResponseCode();InputStream in=status>=200&&status<400?conn.getInputStream():conn.getErrorStream();
            String raw="";
            if(in!=null)try(InputStream source=in;ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
                byte[] b=new byte[1024];int n;while((n=source.read(b))>0){buffer.write(b,0,n);if(buffer.size()>50000)throw new Exception("Respuesta demasiado grande.");}
                raw=buffer.toString(StandardCharsets.UTF_8.name());}
            JSONObject response=raw.isEmpty()?new JSONObject():new JSONObject(raw);
            if(status<200||status>=300)throw new Exception(response.optString("error","No fue posible conectar con la emisora ("+status+")."));
            return response;
        }finally{conn.disconnect();}
    }
}
