package com.tropicalisima.estereo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

final class ThemeManager {
    static class Season {
        final String id,name,phrase;final int accent,second;
        Season(String id,String name,String phrase,int accent,int second){this.id=id;this.name=name;this.phrase=phrase;this.accent=accent;this.second=second;}
    }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("tropicalisima",Context.MODE_PRIVATE);}
    static LocalDate colombia(){return LocalDate.now(ZoneId.of("America/Bogota"));}
    static Season current(Context c){return resolve(c,colombia());}
    static Season resolve(Context c, LocalDate d){
        try(InputStream in=c.getAssets().open("calendario_tropicalisima_app_v1.json")){
            ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1)buf.write(b,0,n);
            JSONObject all=new JSONObject(buf.toString(StandardCharsets.UTF_8.name()));
            JSONObject month=all.getJSONArray("temas_mensuales").getJSONObject(d.getMonthValue()-1);
            String id="mes-"+d.getMonthValue(),name=month.getString("tema"),phrase=month.optString("frase","LA QUE TE PONE A GOZAR");
            int priority=-1;JSONArray events=all.getJSONArray("eventos_especiales");
            for(int i=0;i<events.length();i++){
                JSONObject ev=events.getJSONObject(i);
                if(matches(d,ev.getJSONObject("regla"))&&ev.optInt("prioridad",0)>priority){
                    priority=ev.optInt("prioridad",0);id=ev.getString("id");name=ev.getString("tema");
                    phrase=ev.getString("nombre").toUpperCase(java.util.Locale.ROOT);
                }
            }
            int[] colors=colors(d.getMonthValue(),id);
            return new Season(id,name.toUpperCase(java.util.Locale.ROOT),phrase,colors[0],colors[1]);
        }catch(Exception e){return new Season("clasico","NEÓN TROPICAL","LA QUE TE PONE A GOZAR",Ui.CYAN,Ui.PINK);}
    }
    static boolean matches(LocalDate d,JSONObject rule) throws Exception {
        String kind=rule.getString("tipo");int y=d.getYear();
        switch(kind){
            case "fecha_fija": return d.getMonthValue()==rule.getInt("mes")&&d.getDayOfMonth()==rule.getInt("dia");
            case "rango_anual":{
                JSONObject from=rule.getJSONObject("desde"),to=rule.getJSONObject("hasta");
                int current=d.getMonthValue()*100+d.getDayOfMonth();
                int a=from.getInt("mes")*100+from.getInt("dia"),b=to.getInt("mes")*100+to.getInt("dia");
                return a<=b ? current>=a&&current<=b : current>=a||current<=b;
            }
            case "rango_relativo_pascua":{
                LocalDate easter=easter(y);int lower=rule.getInt("desde_dias"),upper=rule.getInt("hasta_dias");
                return !d.isBefore(easter.plusDays(lower))&&!d.isAfter(easter.plusDays(upper));
            }
            case "n_esimo_dia_semana": {
                int month=rule.getInt("mes"); DayOfWeek w=day(rule.getString("dia_semana"));
                LocalDate desired=LocalDate.of(y,month,1).with(TemporalAdjusters.dayOfWeekInMonth(rule.getInt("n"),w));
                return d.equals(desired);
            }
            case "ultimo_dia_semana": {
                int month=rule.getInt("mes");DayOfWeek w=day(rule.getString("dia_semana"));
                LocalDate desired=LocalDate.of(y,month,1).with(TemporalAdjusters.lastInMonth(w));
                return d.equals(desired);
            }
            default:return false;
        }
    }
    static DayOfWeek day(String s){
        switch(s.toLowerCase(java.util.Locale.ROOT)){
            case "lunes":return DayOfWeek.MONDAY;case "martes":return DayOfWeek.TUESDAY;
            case "miercoles":return DayOfWeek.WEDNESDAY;case "jueves":return DayOfWeek.THURSDAY;
            case "viernes":return DayOfWeek.FRIDAY;case "sabado":return DayOfWeek.SATURDAY;
            default:return DayOfWeek.SUNDAY;
        }
    }
    // Algoritmo de Meeus/Jones/Butcher: Pascua gregoriana, sin Internet.
    static LocalDate easter(int year){
        int a=year%19,b=year/100,c=year%100,d=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3;
        int h=(19*a+b-d-g+15)%30,i=c/4,k=c%4,l=(32+2*e+2*i-h-k)%7,m=(a+11*h+22*l)/451;
        int month=(h+l-7*m+114)/31,day=(h+l-7*m+114)%31+1;
        return LocalDate.of(year,month,day);
    }
    static int[] colors(int month,String special){
        if(special.contains("navidad")||special.contains("nochebuena")||special.contains("velitas")||special.contains("novena"))return new int[]{0xff20e8b3,0xffffb52d};
        if(special.contains("colombia")||special.contains("independencia")||special.contains("boyaca"))return new int[]{0xffffc929,0xff02a9ff};
        if(special.contains("carnaval")||special.contains("feria"))return new int[]{0xffff2999,0xffffa126};
        if(special.contains("amor")||special.contains("madres")||special.contains("valentin"))return new int[]{0xffff3e9a,0xffa94cff};
        if(special.contains("halloween"))return new int[]{0xffb64cff,0xffffa024};
        switch(month){
            case 1:return new int[]{Ui.CYAN,Ui.GOLD};case 2:return new int[]{Ui.PINK,Ui.ORANGE};
            case 3:return new int[]{0xff00dcab,Ui.CYAN};case 4:return new int[]{Ui.CYAN,0xffffaa43};
            case 5:return new int[]{Ui.PINK,Ui.GOLD};case 6:return new int[]{Ui.ORANGE,Ui.CYAN};
            case 7:return new int[]{Ui.GOLD,Ui.CYAN};case 8:return new int[]{Ui.GOLD,Ui.PINK};
            case 9:return new int[]{Ui.PINK,0xffa245ff};case 10:return new int[]{0xffa245ff,Ui.ORANGE};
            case 11:return new int[]{Ui.CYAN,0xff8d52ff};default:return new int[]{0xff34ddb5,Ui.ORANGE};
        }
    }
}
