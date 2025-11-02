package org.spigotmc;

import java.util.concurrent.TimeUnit;

public class CalendarioAPI {

    public static String format(long tempo) {
        tempo -= System.currentTimeMillis();
        if (tempo == 0)
            return "agora mesmo.";

        long meses = TimeUnit.MILLISECONDS.toDays(tempo) / 30;
        long dia;
        if (meses != 0)
            dia = TimeUnit.MILLISECONDS.toDays(tempo) % (meses * 30);
        else
            dia = TimeUnit.MILLISECONDS.toDays(tempo);
        long dias = TimeUnit.MILLISECONDS.toDays(tempo);
        long horas = TimeUnit.MILLISECONDS.toHours(tempo) - (dias * 24);
        long minutos = TimeUnit.MILLISECONDS.toMinutes(tempo) - (TimeUnit.MILLISECONDS.toHours(tempo) * 60);
        long segundos = TimeUnit.MILLISECONDS.toSeconds(tempo) - (TimeUnit.MILLISECONDS.toMinutes(tempo) * 60);

        StringBuilder sb = new StringBuilder();

        if (meses > 0)
            sb.append(meses).append(meses == 1 ? " mes" : " meses");

        if (dia > 0)
            sb.append(meses > 0 ? (horas > 0 ? ", " : " e ") : "").append(dia).append(dia == 1 ? " dia" : " dias");

        if (horas > 0)
            sb.append(dias > 0 ? (minutos > 0 ? ", " : " e ") : "").append(horas).append(horas == 1 ? " hora" : " horas");

        if (minutos > 0)
            sb.append(dias > 0 || horas > 0 ? (segundos > 0 ? ", " : " e ") : "").append(minutos).append(minutos == 1 ? " minuto" : " minutos");

        if (segundos > 0)
            sb.append(dias > 0 || horas > 0 || minutos > 0 ? " e " : (sb.length() > 0 ? ", " : "")).append(segundos).append(segundos == 1 ? " segundo" : " segundos");

        String s = sb.toString();
        return s.isEmpty() ? "agora mesmo" : s;
    }

    public static String format2(long tempo) {
        tempo = System.currentTimeMillis() - tempo;
        if (tempo == 0)
            return "agora mesmo.";

        long meses = TimeUnit.MILLISECONDS.toDays(tempo) / 30;
        long dia;
        if (meses != 0)
            dia = TimeUnit.MILLISECONDS.toDays(tempo) % (meses * 30);
        else
            dia = TimeUnit.MILLISECONDS.toDays(tempo);
        long dias = TimeUnit.MILLISECONDS.toDays(tempo);
        long horas = TimeUnit.MILLISECONDS.toHours(tempo) - (dias * 24);
        long minutos = TimeUnit.MILLISECONDS.toMinutes(tempo) - (TimeUnit.MILLISECONDS.toHours(tempo) * 60);
        long segundos = TimeUnit.MILLISECONDS.toSeconds(tempo) - (TimeUnit.MILLISECONDS.toMinutes(tempo) * 60);

        StringBuilder sb = new StringBuilder();

        if (meses > 0)
            sb.append(meses).append(meses == 1 ? " mes" : " meses");

        if (dia > 0)
            sb.append(meses > 0 ? (horas > 0 ? ", " : " e ") : "").append(dia).append(dia == 1 ? " dia" : " dias");

        if (horas > 0)
            sb.append(dias > 0 ? (minutos > 0 ? ", " : " e ") : "").append(horas).append(horas == 1 ? " hora" : " horas");

        if (minutos > 0)
            sb.append(dias > 0 || horas > 0 ? (segundos > 0 ? ", " : " e ") : "").append(minutos).append(minutos == 1 ? " minuto" : " minutos");

        if (segundos > 0)
            sb.append(dias > 0 || horas > 0 || minutos > 0 ? " e " : (sb.length() > 0 ? ", " : "")).append(segundos).append(segundos == 1 ? " segundo" : " segundos");

        String s = sb.toString();
        return s.isEmpty() ? "agora mesmo" : s;
    }
}
