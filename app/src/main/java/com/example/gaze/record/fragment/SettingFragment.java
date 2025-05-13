package com.example.gaze.record.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.gaze.record.R;
import com.example.gaze.record.app.MyApp;
import com.example.gaze.record.utils.CalculatorUtils;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(Constants.CONFIG);
        addPreferencesFromResource(R.xml.setting_preference);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
//        setPreferencesFromResource(R.xml.setting_preference, rootKey);
    }

    List<EditTextPreference> editTextPreferenceAllList = new ArrayList<>();
    List<EditTextPreference> editTextPreferenceFreeViewing = new ArrayList<>();
    List<EditTextPreference> editTextPreferenceSmoothPursuit = new ArrayList<>();
    List<EditTextPreference> editTextPreferenceFixationStability = new ArrayList<>();
    List<EditTextPreference> editTextPreferenceHorizontalSinusoid = new ArrayList<>();
    List<EditTextPreference> editTextPreferenceVerticalSinusoid = new ArrayList<>();

    @Override
    public void onStart() {
        super.onStart();

        EditTextPreference port = findPreference(Constants.PORT);
        EditTextPreference userId = findPreference(Constants.USER_ID);
        EditTextPreference debugFrontSize = findPreference(Constants.DEBUG_FRONT_SIZE);
        port.setOnPreferenceChangeListener((preference, obj) -> checkPort(obj.toString()));
        userId.setOnPreferenceChangeListener((preference, newValue) -> newValue.toString().length() != 0);
        debugFrontSize.setOnPreferenceChangeListener((preference, newValue) -> isDigital(newValue.toString()));
        EditTextPreference frameDropFreeViewing = findPreference(Constants.FRAME_DROP_FREE_VIEWING);
        EditTextPreference pictureDurationFreeViewing = findPreference(Constants.PICTURE_DURATION_FREE_VIEWING);
//        EditTextPreference stayIntervalFreeViewing = findPreference(Constants.STAY_INTERVAL_FREE_VIEWING);
        editTextPreferenceFreeViewing.add(frameDropFreeViewing);
        editTextPreferenceFreeViewing.add(pictureDurationFreeViewing);
//        editTextPreferenceFreeViewing.add(stayIntervalFreeViewing);

        EditTextPreference frameDropFixationStability = findPreference(Constants.FRAME_DROP_FIXATION_STABILITY);
        EditTextPreference randomCountFixationStability = findPreference(Constants.RANDOM_COUNT_FIXATION_STABILITY);
        EditTextPreference stayIntervalFixationStability = findPreference(Constants.STAY_INTERVAL_FIXATION_STABILITY);
        editTextPreferenceFixationStability.add(frameDropFixationStability);
        editTextPreferenceFixationStability.add(randomCountFixationStability);
        editTextPreferenceFixationStability.add(stayIntervalFixationStability);

        EditTextPreference frameDropHorizontalSinusoid = findPreference(Constants.FRAME_DROP_HORIZONTAL_SINUSOID);
        EditTextPreference freqXHorizontalSinusoid = findPreference(Constants.FREQ_X_HORIZONTAL_SINUSOID);
        EditTextPreference freqYHorizontalSinusoid = findPreference(Constants.FREQ_Y_HORIZONTAL_SINUSOID);
        EditTextPreference phaseXHorizontalSinusoid = findPreference(Constants.PHASE_X_HORIZONTAL_SINUSOID);
        EditTextPreference stayIntervalHorizontalSinusoid = findPreference(Constants.STAY_INTERVAL_HORIZONTAL_SINUSOID);
        editTextPreferenceHorizontalSinusoid.add(frameDropHorizontalSinusoid);
        editTextPreferenceHorizontalSinusoid.add(freqXHorizontalSinusoid);
        editTextPreferenceHorizontalSinusoid.add(freqYHorizontalSinusoid);
        editTextPreferenceHorizontalSinusoid.add(phaseXHorizontalSinusoid);
        editTextPreferenceHorizontalSinusoid.add(stayIntervalHorizontalSinusoid);

        EditTextPreference frameDropVerticalSinusoid = findPreference(Constants.FRAME_DROP_VERTICAL_SINUSOID);
        EditTextPreference freqXVerticalSinusoid = findPreference(Constants.FREQ_X_VERTICAL_SINUSOID);
        EditTextPreference freqYVerticalSinusoid = findPreference(Constants.FREQ_Y_VERTICAL_SINUSOID);
        EditTextPreference phaseYVerticalSinusoid = findPreference(Constants.PHASE_Y_VERTICAL_SINUSOID);
        EditTextPreference stayIntervalVerticalSinusoid = findPreference(Constants.STAY_INTERVAL_VERTICAL_SINUSOID);
        editTextPreferenceVerticalSinusoid.add(frameDropVerticalSinusoid);
        editTextPreferenceVerticalSinusoid.add(freqXVerticalSinusoid);
        editTextPreferenceVerticalSinusoid.add(freqYVerticalSinusoid);
        editTextPreferenceVerticalSinusoid.add(phaseYVerticalSinusoid);
        editTextPreferenceVerticalSinusoid.add(stayIntervalVerticalSinusoid);


        EditTextPreference frameDropSmoothPursuit = findPreference(Constants.FRAME_DROP_SMOOTH_PURSUIT);
        EditTextPreference freqXSmoothPursuit = findPreference(Constants.FREQ_X_SMOOTH_PURSUIT);
        EditTextPreference freqYSmoothPursuit = findPreference(Constants.FREQ_Y_SMOOTH_PURSUIT);
        EditTextPreference phaseXSmoothPursuit = findPreference(Constants.PHASE_X_SMOOTH_PURSUIT);
        EditTextPreference phaseYSmoothPursuit = findPreference(Constants.PHASE_Y_SMOOTH_PURSUIT);
        EditTextPreference stayIntervalSmoothPursuit = findPreference(Constants.STAY_INTERVAL_SMOOTH_PURSUIT);
        editTextPreferenceSmoothPursuit.add(frameDropSmoothPursuit);
        editTextPreferenceSmoothPursuit.add(freqXSmoothPursuit);
        editTextPreferenceSmoothPursuit.add(freqYSmoothPursuit);
        editTextPreferenceSmoothPursuit.add(phaseXSmoothPursuit);
        editTextPreferenceSmoothPursuit.add(phaseYSmoothPursuit);
        editTextPreferenceSmoothPursuit.add(stayIntervalSmoothPursuit);


        editTextPreferenceAllList.addAll(editTextPreferenceFreeViewing);
        editTextPreferenceAllList.addAll(editTextPreferenceFixationStability);
        editTextPreferenceAllList.addAll(editTextPreferenceHorizontalSinusoid);
        editTextPreferenceAllList.addAll(editTextPreferenceVerticalSinusoid);
        editTextPreferenceAllList.addAll(editTextPreferenceSmoothPursuit);

        for (EditTextPreference editTextPreference: editTextPreferenceAllList) {
            editTextPreference.setOnPreferenceChangeListener((preference, newValue) -> isFloat(newValue.toString()));
        }


        ListPreference listPreferenceMotionType = findPreference(Constants.MOTION_TYPE);
        listPreferenceMotionType.setOnPreferenceChangeListener((preference, newValue) -> {
            if ("FreeViewing".equals(newValue.toString())) {
                disableAll();
                enableList(editTextPreferenceFreeViewing);
            } else if ("SmoothPursuit".equals(newValue.toString())) {
                disableAll();
                enableList(editTextPreferenceSmoothPursuit);
            } else if ("HorizontalSinusoid".equals(newValue.toString())) {
                disableAll();
                enableList(editTextPreferenceHorizontalSinusoid);
            } else if ("VerticalSinusoid".equals(newValue.toString())) {
                disableAll();
                enableList(editTextPreferenceVerticalSinusoid);
            } else if ("FixationStability".equals(newValue.toString())) {
                disableAll();
                enableList(editTextPreferenceFixationStability);
            }
            return true;
        });

        // initial
        String newValue = SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing");
        Log.e(getClass().getSimpleName(), newValue);
        if ("FreeViewing".equals(newValue)) {
            disableAll();
            enableList(editTextPreferenceFreeViewing);
        } else if ("SmoothPursuit".equals(newValue)) {
            disableAll();
            enableList(editTextPreferenceSmoothPursuit);
        } else if ("HorizontalSinusoid".equals(newValue)) {
            disableAll();
            enableList(editTextPreferenceHorizontalSinusoid);
        } else if ("VerticalSinusoid".equals(newValue)) {
            disableAll();
            enableList(editTextPreferenceVerticalSinusoid);
        } else if ("FixationStability".equals(newValue)) {
            disableAll();
            enableList(editTextPreferenceFixationStability);
        }
    }


    private void disableAll() {
        for (EditTextPreference editTextPreference : editTextPreferenceAllList) {
            editTextPreference.setEnabled(false);
        }
    }

    private void enableList(List<EditTextPreference> editTextPreferenceList) {
        for (EditTextPreference editTextPreference : editTextPreferenceList) {
            editTextPreference.setEnabled(true);
        }
    }


    @SuppressLint("DefaultLocale")
    private boolean isFloat(String value) {
        if (value != null && value.length() > 0) {
            try {
                double res = CalculatorUtils.conversion(value);
                if (!Double.isNaN(res)) {
                    Toast.makeText(MyApp.getInstance(), String.format("表达式的结果为：%.4f", res), Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    Toast.makeText(MyApp.getInstance(), "不是合法的表达式或者数字: " + value + "，请检查", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (Exception e) {
                Toast.makeText(MyApp.getInstance(), "不是合法的表达式或者数字: " + value + "，请检查", Toast.LENGTH_SHORT).show();
                return false;
            }

        }
        Toast.makeText(MyApp.getInstance(), "未输入内容，请检查", Toast.LENGTH_SHORT).show();
        return false;
    }


    private boolean checkPort(String port) {
        if ("".equals(port) || port.length() > 5) {
            Toast.makeText(MyApp.getInstance(), "端口输入错误，请检查", Toast.LENGTH_SHORT).show();
            return false;
        }
        for (int i = 0; i < port.length(); i++) {
            if (!Character.isDigit(port.charAt(i))) {
                Toast.makeText(MyApp.getInstance(), "端口输入错误，请检查", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private boolean isDigital(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                Toast.makeText(MyApp.getInstance(), "输入的不是整数，请检查", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

}
