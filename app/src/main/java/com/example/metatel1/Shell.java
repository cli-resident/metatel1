package com.example.metatel1;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class Shell {

    private String name;
    private int speed;
    private double radius;

    public String getName() { return name; }
    public int getSpeed() { return speed; }
    public double getRadius() { return radius; }

    public static List<Shell> loadAll(Context context) {
        try {
            InputStream is = context.getAssets().open("shells.json");
            InputStreamReader reader = new InputStreamReader(is);

            Type listType = new TypeToken<List<Shell>>() {}.getType();
            return new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Shell getByName(Context context, String shellName) {
        List<Shell> shells = loadAll(context);
        if (shells == null) return null;
        for (Shell shell : shells) {
            if (shell.getName().equalsIgnoreCase(shellName)) {
                return shell;
            }
        }
        return null;
    }
}
