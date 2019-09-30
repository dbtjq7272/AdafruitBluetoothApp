package com.adafruit.bluefruit.le.connect.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adafruit.bluefruit.le.connect.R;
import com.adafruit.bluefruit.le.connect.ble.BleUtils;
import com.adafruit.bluefruit.le.connect.ble.UartPacket;
import com.adafruit.bluefruit.le.connect.ble.UartPacketManagerBase;
import com.adafruit.bluefruit.le.connect.ble.central.BlePeripheralUart;
import com.adafruit.bluefruit.le.connect.mqtt.MqttManager;
import com.adafruit.bluefruit.le.connect.mqtt.MqttSettings;
import com.adafruit.bluefruit.le.connect.utils.KeyboardUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimerTask;
import java.util.concurrent.Delayed;

import static android.speech.tts.TextToSpeech.ERROR;
import static java.lang.System.currentTimeMillis;

// TODO: register
public abstract class UartBaseFragment extends ConnectedPeripheralFragment implements UartPacketManagerBase.Listener, MqttManager.MqttManagerListener {
    // Log
    private final static String TAG = UartBaseFragment.class.getSimpleName();

    // Configuration
    public final static int kDefaultMaxPacketsToPaintAsText = 500;
    private final static int kInfoColor = Color.parseColor("#F21625");

    // Constants
    private final static String kPreferences = "UartActivity_prefs";
    private final static String kPreferences_eol = "eol";
    private final static String kPreferences_eolCharactersId = "eolCharactersId";
    private final static String kPreferences_echo = "echo";
    private final static String kPreferences_asciiMode = "ascii";
    private final static String kPreferences_timestampDisplayMode = "timestampdisplaymode";

    //State
    private final int StateWaiting = 0;
    private final int StateWorking = 1;
    private final int StatePause = 2;
    private final int StateComplete = 3;

    //Data UI
    String protocol;
    private LinearLayout mWork1;
    private TextView mWork1_nameTextView;
    private TextView mWork1_stateTextView;
    private TextView mWork1_content1TextView;
    private TextView mWork1_content2TextView;
    private TextView mWork1_content3TextView;
    private TextView mWork1_content4TextView;
    private TextView mWork1_content5TextView;
    private TextView mWork1_timeTextView;
    private TextView mWork1_protocolTextView;

    private LinearLayout mWork2;
    private TextView mWork2_nameTextView;
    private TextView mWork2_stateTextView;
    private TextView mWork2_content1TextView;
    private TextView mWork2_content2TextView;
    private TextView mWork2_content3TextView;
    private TextView mWork2_content4TextView;
    private TextView mWork2_content5TextView;
    private TextView mWork2_timeTextView;
    private TextView mWork2_protocolTextView;

    private LinearLayout mWork3;
    private TextView mWork3_nameTextView;
    private TextView mWork3_stateTextView;
    private TextView mWork3_content1TextView;
    private TextView mWork3_content2TextView;
    private TextView mWork3_content3TextView;
    private TextView mWork3_content4TextView;
    private TextView mWork3_content5TextView;
    private TextView mWork3_timeTextView;
    private TextView mWork3_protocolTextView;

    private LinearLayout mWork4;
    private TextView mWork4_nameTextView;
    private TextView mWork4_stateTextView;
    private TextView mWork4_content1TextView;
    private TextView mWork4_content2TextView;
    private TextView mWork4_content3TextView;
    private TextView mWork4_content4TextView;
    private TextView mWork4_content5TextView;
    private TextView mWork4_timeTextView;
    private TextView mWork4_protocolTextView;

    //Data infomation
    private int Work1_state;
    private int Work2_state;
    private int Work3_state;
    private int Work4_state;

    //Time Thread
    Handler Work1_handler;
    Handler Work2_handler;
    Handler Work3_handler;
    Handler Work4_handler;


    long start_Work1time;
    long pause_Work1time;

    long start_Work2time;
    long pause_Work2time;

    long start_Work3time;
    long pause_Work3time;

    long start_Work4time;
    long pause_Work4time;

    //TTS
    private TextToSpeech tts_Work1;
    private TextToSpeech tts_Work2;
    private TextToSpeech tts_Work3;
    private TextToSpeech tts_Work4;


    // UI
    private EditText mBufferTextView;
    private RecyclerView mBufferRecylerView;
    protected TimestampItemAdapter mBufferItemAdapter;
    private EditText mSendEditText;
    private Button mSendButton;
    private MenuItem mMqttMenuItem;
    private Handler mMqttMenuItemAnimationHandler;
    private TextView mSentBytesTextView;
    private TextView mReceivedBytesTextView;
    protected Spinner mSendPeripheralSpinner;

    // UI TextBuffer (refreshing the text buffer is managed with a timer because a lot of changes can arrive really fast and could stall the main thread)
    private Handler mUIRefreshTimerHandler = new Handler();
    private Runnable mUIRefreshTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUITimerRunning) {
                reloadData();
                // Log.d(TAG, "updateDataUI");
                mUIRefreshTimerHandler.postDelayed(this, 200);
            }
        }
    };
    private boolean isUITimerRunning = false;

    // Data
    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());
    protected UartPacketManagerBase mUartData;
    protected List<BlePeripheralUart> mBlePeripheralsUart = new ArrayList<>();

    private boolean mShowDataInHexFormat;
    private boolean mIsTimestampDisplayMode;
    private boolean mIsEchoEnabled;
    private boolean mIsEolEnabled;
    private int mEolCharactersId;

    protected volatile SpannableStringBuilder mTextSpanBuffer = new SpannableStringBuilder();

    protected MqttManager mMqttManager;

    private int maxPacketsToPaintAsText;
    private int mPacketsCacheLastSize = 0;

    // region Fragment Lifecycle
    public UartBaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Context context = getContext();

        // Buffer recycler view
        if (context != null) {
            mBufferRecylerView = view.findViewById(R.id.bufferRecyclerView);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            Drawable lineSeparatorDrawable = ContextCompat.getDrawable(context, R.drawable.simpledivideritemdecoration);
            assert lineSeparatorDrawable != null;
            itemDecoration.setDrawable(lineSeparatorDrawable);
            mBufferRecylerView.addItemDecoration(itemDecoration);

            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            //layoutManager.setStackFromEnd(true);        // Scroll to bottom when adding elements
            mBufferRecylerView.setLayoutManager(layoutManager);

            ((SimpleItemAnimator) mBufferRecylerView.getItemAnimator()).setSupportsChangeAnimations(false);         // Disable update animation
            mBufferItemAdapter = new TimestampItemAdapter(context);            // Adapter

            mBufferRecylerView.setAdapter(mBufferItemAdapter);
        }

        //Data Setting
        Work1_state = StateWaiting;
        Work2_state = StateWaiting;
        Work3_state = StateWaiting;
        Work4_state = StateWaiting;

        //Data Rcv
        mWork1 = view.findViewById(R.id.work1);
        mWork1_nameTextView = view.findViewById(R.id.work1_name);
        mWork1_stateTextView = view.findViewById(R.id.work1_state);
        mWork1_content1TextView = view.findViewById(R.id.work1_content1);
        mWork1_content2TextView = view.findViewById(R.id.work1_content2);
        mWork1_content3TextView = view.findViewById(R.id.work1_content3);
        mWork1_content4TextView = view.findViewById(R.id.work1_content4);
        mWork1_content5TextView = view.findViewById(R.id.work1_content5);
        mWork1_timeTextView = view.findViewById(R.id.work1_time);
        mWork1_protocolTextView = view.findViewById(R.id.work1_protocol);

        //Data Rcv
        mWork2 = view.findViewById(R.id.work2);
        mWork2_nameTextView = view.findViewById(R.id.work2_name);
        mWork2_stateTextView = view.findViewById(R.id.work2_state);
        mWork2_content1TextView = view.findViewById(R.id.work2_content1);
        mWork2_content2TextView = view.findViewById(R.id.work2_content2);
        mWork2_content3TextView = view.findViewById(R.id.work2_content3);
        mWork2_content4TextView = view.findViewById(R.id.work2_content4);
        mWork2_content5TextView = view.findViewById(R.id.work2_content5);
        mWork2_timeTextView = view.findViewById(R.id.work2_time);
        mWork2_protocolTextView = view.findViewById(R.id.work2_protocol);

        //Data Rcv
        mWork3 = view.findViewById(R.id.work3);
        mWork3_nameTextView = view.findViewById(R.id.work3_name);
        mWork3_stateTextView = view.findViewById(R.id.work3_state);
        mWork3_content1TextView = view.findViewById(R.id.work3_content1);
        mWork3_content2TextView = view.findViewById(R.id.work3_content2);
        mWork3_content3TextView = view.findViewById(R.id.work3_content3);
        mWork3_content4TextView = view.findViewById(R.id.work3_content4);
        mWork3_content5TextView = view.findViewById(R.id.work3_content5);
        mWork3_timeTextView = view.findViewById(R.id.work3_time);
        mWork3_protocolTextView = view.findViewById(R.id.work3_protocol);

        mWork4 = view.findViewById(R.id.work4);
        mWork4_nameTextView = view.findViewById(R.id.work4_name);
        mWork4_stateTextView = view.findViewById(R.id.work4_state);
        mWork4_content1TextView = view.findViewById(R.id.work4_content1);
        mWork4_content2TextView = view.findViewById(R.id.work4_content2);
        mWork4_content3TextView = view.findViewById(R.id.work4_content3);
        mWork4_content4TextView = view.findViewById(R.id.work4_content4);
        mWork4_content5TextView = view.findViewById(R.id.work4_content5);
        mWork4_timeTextView = view.findViewById(R.id.work4_time);
        mWork4_protocolTextView = view.findViewById(R.id.work4_protocol);

        Work1_handler = new Handler();
        Work2_handler = new Handler();
        Work3_handler = new Handler();
        Work4_handler = new Handler();

        // TTS
        tts_Work1 = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    tts_Work1.setLanguage(Locale.KOREAN);
                }
            }
        });

        tts_Work2 = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    tts_Work2.setLanguage(Locale.KOREAN);
                }
            }
        });

        tts_Work3 = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    tts_Work3.setLanguage(Locale.KOREAN);
                }
            }
        });

        tts_Work4 = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    tts_Work4.setLanguage(Locale.KOREAN);
                }
            }
        });
        // Buffer
        mBufferTextView = view.findViewById(R.id.bufferTextView);
        if (mBufferTextView != null) {
            mBufferTextView.setKeyListener(null);     // make it not editable
        }

        // Send Text
//        mSendEditText = view.findViewById(R.id.sendEditText);
//        mSendEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
//            if (actionId == EditorInfo.IME_ACTION_SEND) {
//                onClickSend();
//                return true;
//            }
//            return false;
//        });
//        mSendEditText.setOnFocusChangeListener((view1, hasFocus) -> {
//            if (!hasFocus) {
//                // Dismiss keyboard when sendEditText loses focus
//                KeyboardUtils.dismissKeyboard(view1);
//            }
//        });

        mSendButton = view.findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(view12 -> onClickSend());

        final boolean isInMultiUartMode = isInMultiUartMode();
        mSendPeripheralSpinner = view.findViewById(R.id.sendPeripheralSpinner);
        mSendPeripheralSpinner.setVisibility(isInMultiUartMode ? View.VISIBLE : View.GONE);

        // Counters
        mSentBytesTextView = view.findViewById(R.id.sentBytesTextView);
        mReceivedBytesTextView = view.findViewById(R.id.receivedBytesTextView);

        // Read shared preferences
        maxPacketsToPaintAsText = kDefaultMaxPacketsToPaintAsText; //PreferencesFragment.getUartTextMaxPackets(this);

        // Read local preferences
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
            setShowDataInHexFormat(!preferences.getBoolean(kPreferences_asciiMode, true));
            final boolean isTimestampDisplayMode = preferences.getBoolean(kPreferences_timestampDisplayMode, false);
            setDisplayFormatToTimestamp(isTimestampDisplayMode);
            setEchoEnabled(preferences.getBoolean(kPreferences_echo, true));
            mIsEolEnabled = preferences.getBoolean(kPreferences_eol, true);
            mEolCharactersId = preferences.getInt(kPreferences_eolCharactersId, 0);
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();        // update options menu with current values
            }

            // Mqtt init
            if (mMqttManager == null) {
                mMqttManager = new MqttManager(context, this);
                if (MqttSettings.isConnected(context)) {
                    mMqttManager.connectFromSavedSettings();
                }
            } else {
                mMqttManager.setListener(this);
            }
        }
    }

    private void setShowDataInHexFormat(boolean showDataInHexFormat) {
        mShowDataInHexFormat = showDataInHexFormat;
        mBufferItemAdapter.setShowDataInHexFormat(showDataInHexFormat);

    }

    private void setEchoEnabled(boolean isEchoEnabled) {
        mIsEchoEnabled = isEchoEnabled;
        mBufferItemAdapter.setEchoEnabled(isEchoEnabled);
    }

    abstract protected boolean isInMultiUartMode();

    @Override
    public void onResume() {
        super.onResume();

        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        updateMqttStatus();

        updateBytesUI();

        isUITimerRunning = true;
        mUIRefreshTimerHandler.postDelayed(mUIRefreshTimerRunnable, 0);
    }

    @Override
    public void onPause() {
        super.onPause();

        //Log.d(TAG, "remove ui timer");
        isUITimerRunning = false;
        mUIRefreshTimerHandler.removeCallbacksAndMessages(mUIRefreshTimerRunnable);

        // Save preferences
        final Context context = getContext();
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(kPreferences_echo, mIsEchoEnabled);
            editor.putBoolean(kPreferences_eol, mIsEolEnabled);
            editor.putInt(kPreferences_eolCharactersId, mEolCharactersId);
            editor.putBoolean(kPreferences_asciiMode, !mShowDataInHexFormat);
            editor.putBoolean(kPreferences_timestampDisplayMode, mIsTimestampDisplayMode);

            editor.apply();
        }
    }

    @Override
    public void onDestroy() {
        mUartData = null;

        // Disconnect mqtt
        if (mMqttManager != null) {
            mMqttManager.disconnect();
        }

        // Uart
        if (mBlePeripheralsUart != null) {
            for (BlePeripheralUart blePeripheralUart : mBlePeripheralsUart) {
                blePeripheralUart.uartDisable();
            }
            mBlePeripheralsUart.clear();
            mBlePeripheralsUart = null;
        }

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_uart, menu);

        // Mqtt
        mMqttMenuItem = menu.findItem(R.id.action_mqttsettings);
        mMqttMenuItemAnimationHandler = new Handler();
        mMqttMenuItemAnimationRunnable.run();

        // DisplayMode
        MenuItem displayModeMenuItem = menu.findItem(R.id.action_displaymode);
        displayModeMenuItem.setTitle(String.format("%s: %s", getString(R.string.uart_settings_displayMode_title), getString(mIsTimestampDisplayMode ? R.string.uart_settings_displayMode_timestamp : R.string.uart_settings_displayMode_text)));
        SubMenu displayModeSubMenu = displayModeMenuItem.getSubMenu();
        if (mIsTimestampDisplayMode) {
            MenuItem displayModeTimestampMenuItem = displayModeSubMenu.findItem(R.id.action_displaymode_timestamp);
            displayModeTimestampMenuItem.setChecked(true);
        } else {
            MenuItem displayModeTextMenuItem = displayModeSubMenu.findItem(R.id.action_displaymode_text);
            displayModeTextMenuItem.setChecked(true);
        }

        // DataMode
        MenuItem dataModeMenuItem = menu.findItem(R.id.action_datamode);
        dataModeMenuItem.setTitle(String.format("%s: %s", getString(R.string.uart_settings_dataMode_title), getString(mShowDataInHexFormat ? R.string.uart_settings_dataMode_hex : R.string.uart_settings_dataMode_ascii)));
        SubMenu dataModeSubMenu = dataModeMenuItem.getSubMenu();
        if (mShowDataInHexFormat) {
            MenuItem dataModeHexMenuItem = dataModeSubMenu.findItem(R.id.action_datamode_hex);
            dataModeHexMenuItem.setChecked(true);
        } else {
            MenuItem dataModeAsciiMenuItem = dataModeSubMenu.findItem(R.id.action_datamode_ascii);
            dataModeAsciiMenuItem.setChecked(true);
        }

        // Echo
        MenuItem echoMenuItem = menu.findItem(R.id.action_echo);
        echoMenuItem.setTitle(R.string.uart_settings_echo_title);
        echoMenuItem.setChecked(mIsEchoEnabled);

        // Eol
        MenuItem eolMenuItem = menu.findItem(R.id.action_eol);
        eolMenuItem.setTitle(R.string.uart_settings_eol_title);
        eolMenuItem.setChecked(mIsEolEnabled);

        // Eol Characters
        MenuItem eolModeMenuItem = menu.findItem(R.id.action_eolmode);
        eolModeMenuItem.setTitle(String.format("%s: %s", getString(R.string.uart_settings_eolCharacters_title), getString(getEolCharactersStringId())));
        SubMenu eolModeSubMenu = eolModeMenuItem.getSubMenu();
        int selectedEolCharactersSubMenuId;
        switch (mEolCharactersId) {
            case 1:
                selectedEolCharactersSubMenuId = R.id.action_eolmode_r;
                break;
            case 2:
                selectedEolCharactersSubMenuId = R.id.action_eolmode_nr;
                break;
            case 3:
                selectedEolCharactersSubMenuId = R.id.action_eolmode_rn;
                break;
            default:
                selectedEolCharactersSubMenuId = R.id.action_eolmode_n;
                break;
        }
        MenuItem selectedEolCharacterMenuItem = eolModeSubMenu.findItem(selectedEolCharactersSubMenuId);
        selectedEolCharacterMenuItem.setChecked(true);


    }


    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();

        mMqttMenuItemAnimationHandler.removeCallbacks(mMqttMenuItemAnimationRunnable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return super.onOptionsItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.action_help: {
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                if (fragmentManager != null) {
                    CommonHelpFragment helpFragment = CommonHelpFragment.newInstance(getString(R.string.uart_help_title), getString(R.string.uart_help_text_android));
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                            .replace(R.id.contentLayout, helpFragment, "Help");
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
                return true;
            }

            case R.id.action_mqttsettings: {
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                if (fragmentManager != null) {
                    MqttSettingsFragment mqttSettingsFragment = MqttSettingsFragment.newInstance();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                            .replace(R.id.contentLayout, mqttSettingsFragment, "MqttSettings");
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
                return true;
            }

            case R.id.action_displaymode_timestamp: {
                setDisplayFormatToTimestamp(true);
                invalidateTextView();
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_displaymode_text: {
                setDisplayFormatToTimestamp(false);
                invalidateTextView();
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_datamode_hex: {
                setShowDataInHexFormat(true);
                invalidateTextView();
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_datamode_ascii: {
                setShowDataInHexFormat(false);
                invalidateTextView();
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_echo: {
                setEchoEnabled(!mIsEchoEnabled);
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_eol: {
                mIsEolEnabled = !mIsEolEnabled;
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_eolmode_n: {
                mEolCharactersId = 0;
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_eolmode_r: {
                mEolCharactersId = 1;
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_eolmode_nr: {
                mEolCharactersId = 2;
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_eolmode_rn: {
                mEolCharactersId = 3;
                activity.invalidateOptionsMenu();
                return true;
            }

            case R.id.action_export: {
                export();
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // endregion

    // region Uart
    protected abstract void setupUart();

    protected abstract void send(String message);

    private void onClickSend() {
        String newText = mSendEditText.getText().toString();
        mSendEditText.setText("");       // Clear editText

        // Add eol
        if (mIsEolEnabled) {
            // Add newline character if checked
            newText += getEolCharacters();
        }

        send(newText);
    }

    // endregion

    // region UI
    protected void updateUartReadyUI(boolean isReady) {
        // Check null because crash detected in logs
        if (mSendEditText != null) {
            mSendEditText.setEnabled(isReady);
        }
        if (mSendButton != null) {
            mSendButton.setEnabled(isReady);
        }
    }

    private void addTextToSpanBuffer(SpannableStringBuilder spanBuffer, String text, int color, boolean isBold) {
        final int from = spanBuffer.length();
        spanBuffer.append(text);
        spanBuffer.setSpan(new ForegroundColorSpan(color), from, from + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (isBold) {
            spanBuffer.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), from, from + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @MainThread
    private void updateBytesUI() {
        if (mUartData != null) {
            mSentBytesTextView.setText(String.format(getString(R.string.uart_sentbytes_format), mUartData.getSentBytes()));
            mReceivedBytesTextView.setText(String.format(getString(R.string.uart_receivedbytes_format), mUartData.getReceivedBytes()));
        }
    }

    private void setDisplayFormatToTimestamp(boolean enabled) {
        mIsTimestampDisplayMode = enabled;
        mBufferTextView.setVisibility(enabled ? View.GONE : View.VISIBLE);
        mBufferRecylerView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    abstract protected int colorForPacket(UartPacket packet);

    private boolean isFontBoldForPacket(UartPacket packet) {
        return packet.getMode() == UartPacket.TRANSFERMODE_TX;
    }

    private void invalidateTextView() {
        if (!mIsTimestampDisplayMode) {
            mPacketsCacheLastSize = 0;
            mTextSpanBuffer.clear();
            mBufferTextView.setText("");
        }
    }
    private void reloadData() {
        List<UartPacket> packetsCache = mUartData.getPacketsCache();
        final int packetsCacheSize = packetsCache.size();
        if (mPacketsCacheLastSize != packetsCacheSize) {        // Only if the buffer has changed

            if (mIsTimestampDisplayMode) {

                mBufferItemAdapter.notifyDataSetChanged();
                final int bufferSize = mBufferItemAdapter.getCachedDataBufferSize();
                mBufferRecylerView.smoothScrollToPosition(Math.max(bufferSize - 1, 0));

            } else {
                if (packetsCacheSize > maxPacketsToPaintAsText) {
                    mPacketsCacheLastSize = packetsCacheSize - maxPacketsToPaintAsText;
                    mTextSpanBuffer.clear();
                    addTextToSpanBuffer(mTextSpanBuffer, getString(R.string.uart_text_dataomitted) + "\n", kInfoColor, false);
                }

                // Log.d(TAG, "update packets: "+(bufferSize-mPacketsCacheLastSize));
                for (int i = mPacketsCacheLastSize; i < packetsCacheSize; i++) {
                    final UartPacket packet = packetsCache.get(i);
                    onUartPacketText(packet);
                }



//Start
                protocol = getItem(mTextSpanBuffer.toString());
//                Toast.makeText(getContext(), "프로토콜을 입력하여 주십시오", Toast.LENGTH_LONG).show();

                if (protocol.equals(mWork1_protocolTextView.getText().toString())){
                    Log.d(TAG, protocol + " ->");
                    if (!mWork1_timeTextView.getText().toString().equals("")) {
                        Runnable runnablecode = new Runnable() {
                            @Override
                            public void run() {
                                tts_Work1.speak("작업"+ mWork1_nameTextView.getText().toString() +" 완료 되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                                mWork1.setBackgroundResource(R.color.blue_50);
                                mWork1_stateTextView.setText(R.string.working_state_complete);
                            }
                        };
                        int time = Integer.parseInt(mWork1_timeTextView.getText().toString()) * 1000;
                        int count = getContentCount(mWork1_content1TextView, mWork1_content2TextView, mWork1_content3TextView, mWork1_content4TextView, mWork1_content5TextView);
                        if (count == 0){
                            Toast.makeText(getContext(), "작업" + mWork1_nameTextView.getText().toString()+" 의 작업 내용을입력하여 주십시오", Toast.LENGTH_LONG).show();
                        }
                        if (Work1_state == StateWaiting) {
                            long start = currentTimeMillis();
                            Log.d(TAG, "waiting -> working");
                            Work1_state = StateWorking;

                            tts_Work1.setSpeechRate(1.3f);
                            tts_Work1.speak("작업" + mWork1_nameTextView.getText().toString() + " 시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork1_stateTextView.setText(R.string.working_state_working);
                            mWork1.setBackgroundResource(R.color.green_50);

                            Work1_handler.postDelayed(runnablecode, time*count);
                            start_Work1time = currentTimeMillis();
                        }

                        else if(Work1_state == StatePause){
                            Work1_handler.postDelayed(runnablecode, time*count-pause_Work1time);
                            Log.d(TAG, "pause -> working");
                            Work1_state = StateWorking;
                            tts_Work1.setSpeechRate(1.3f);
                            tts_Work1.speak("작업" + mWork1_nameTextView.getText().toString() + " 재시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork1_stateTextView.setText(R.string.working_state_working);
                            mWork1.setBackgroundResource(R.color.green_50);

                        }

                        else if (Work1_state == StateWorking) {
                            pause_Work1time = currentTimeMillis()-start_Work1time;
                            if(Work1_handler != null) {Work1_handler.removeMessages(0);}
                            Log.d(TAG, "working -> pause");
                            Work1_state = StatePause;
                            tts_Work1.setSpeechRate(1.3f);
                            tts_Work1.speak("작업" + mWork1_nameTextView.getText().toString() + " 일시정지되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork1_stateTextView.setText(R.string.working_state_pause);
                            mWork1.setBackgroundResource(R.color.red_50);
                        }
                    }

                    else{
                        Toast.makeText(getContext(), "작업 " + mWork1_nameTextView.getText().toString() +"의 시간을 입력하여 주십시오", Toast.LENGTH_LONG).show();
                    }
                }

                else if (protocol.equals(mWork2_protocolTextView.getText().toString())){
                    Log.d(TAG, protocol + " ->");
                    if (!mWork2_timeTextView.getText().toString().equals("")) {
                        Runnable runnablecode = new Runnable() {
                            @Override
                            public void run() {
                                tts_Work2.speak("작업"+ mWork2_nameTextView.getText().toString() +" 완료 되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                                mWork2.setBackgroundResource(R.color.blue_50);
                                mWork2_stateTextView.setText(R.string.working_state_complete);
                            }
                        };
                        int time = Integer.parseInt(mWork2_timeTextView.getText().toString()) * 1000;
                        int count = getContentCount(mWork2_content1TextView, mWork2_content2TextView, mWork2_content3TextView, mWork2_content4TextView, mWork2_content5TextView);

                        if (Work2_state == StateWaiting) {
                            long start = currentTimeMillis();
                            Log.d(TAG, "waiting -> working");
                            Work2_state = StateWorking;

                            tts_Work2.setSpeechRate(1.3f);
                            tts_Work2.speak("작업" + mWork2_nameTextView.getText().toString() + " 시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork2_stateTextView.setText(R.string.working_state_working);
                            mWork2.setBackgroundResource(R.color.green_50);

                            Work2_handler.postDelayed(runnablecode, time*count);
                            start_Work2time = currentTimeMillis();
                        }

                        else if(Work2_state == StatePause){
                            Work2_handler.postDelayed(runnablecode, time*count-pause_Work2time);
                            Log.d(TAG, "pause -> working");
                            Work2_state = StateWorking;
                            tts_Work2.setSpeechRate(1.3f);
                            tts_Work2.speak("작업" + mWork2_nameTextView.getText().toString() + " 재시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork2_stateTextView.setText(R.string.working_state_working);
                            mWork2.setBackgroundResource(R.color.green_50);

                        }

                        else if (Work2_state == StateWorking) {
                            pause_Work2time = currentTimeMillis()-start_Work2time;
                            if(Work2_handler != null) {Work2_handler.removeMessages(0);}
                            Log.d(TAG, "working -> pause");
                            Work2_state = StatePause;
                            tts_Work2.setSpeechRate(1.3f);
                            tts_Work2.speak("작업" + mWork2_nameTextView.getText().toString() + " 일시정되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork2_stateTextView.setText(R.string.working_state_pause);
                            mWork2.setBackgroundResource(R.color.red_50);
                        }
                    }

                    else{
                        Toast.makeText(getContext(), "작업 " + mWork2_nameTextView.getText().toString() +"의 시간을 입력하여 주십시오", Toast.LENGTH_LONG).show();
                    }
                }

                else if (protocol.equals(mWork3_protocolTextView.getText().toString())){
                    Log.d(TAG, protocol + " ->");
                    if (!mWork3_timeTextView.getText().toString().equals("")) {
                        Runnable runnablecode = new Runnable() {
                            @Override
                            public void run() {
                                tts_Work3.speak("작업"+ mWork3_nameTextView.getText().toString() +" 완료 되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                                mWork3.setBackgroundResource(R.color.blue_50);
                                mWork3_stateTextView.setText(R.string.working_state_complete);
                            }
                        };
                        int time = Integer.parseInt(mWork3_timeTextView.getText().toString()) * 1000;
                        int count = getContentCount(mWork3_content1TextView, mWork3_content2TextView, mWork3_content3TextView, mWork3_content4TextView, mWork3_content5TextView);

                        if (Work3_state == StateWaiting) {
                            long start = currentTimeMillis();
                            Log.d(TAG, "waiting -> working");
                            Work3_state = StateWorking;

                            tts_Work3.setSpeechRate(1.3f);
                            tts_Work3.speak("작업" + mWork3_nameTextView.getText().toString() + " 시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork3_stateTextView.setText(R.string.working_state_working);
                            mWork3.setBackgroundResource(R.color.green_50);

                            Work3_handler.postDelayed(runnablecode, time*count);
                            start_Work3time = currentTimeMillis();
                        }

                        else if(Work3_state == StatePause){
                            Work3_handler.postDelayed(runnablecode, time*count-pause_Work3time);
                            Log.d(TAG, "pause -> working");
                            Work3_state = StateWorking;
                            tts_Work3.setSpeechRate(1.3f);
                            tts_Work3.speak("작업" + mWork3_nameTextView.getText().toString() + " 재시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork3_stateTextView.setText(R.string.working_state_working);
                            mWork3.setBackgroundResource(R.color.green_50);

                        }

                        else if (Work3_state == StateWorking) {
                            pause_Work3time = currentTimeMillis()-start_Work3time;
                            if(Work3_handler != null) {Work3_handler.removeMessages(0);}
                            Log.d(TAG, "working -> pause");
                            Work3_state = StatePause;
                            tts_Work3.setSpeechRate(1.3f);
                            tts_Work3.speak("작업" + mWork3_nameTextView.getText().toString() + " 일시정지되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork3_stateTextView.setText(R.string.working_state_pause);
                            mWork3.setBackgroundResource(R.color.red_50);
                        }
                    }

                    else{
                        Toast.makeText(getContext(), "작업 " + mWork3_nameTextView.getText().toString() +"의 시간을 입력하여 주십시오", Toast.LENGTH_LONG).show();
                    }
                }

                else if (protocol.equals(mWork4_protocolTextView.getText().toString())){
                    Log.d(TAG, protocol + " ->");
                    if (!mWork4_timeTextView.getText().toString().equals("")) {
                        Runnable runnablecode = new Runnable() {
                            @Override
                            public void run() {
                                tts_Work4.speak("작업"+ mWork4_nameTextView.getText().toString() +" 완료 되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                                mWork4.setBackgroundResource(R.color.blue_50);
                                mWork4_stateTextView.setText(R.string.working_state_complete);
                            }
                        };
                        int time = Integer.parseInt(mWork4_timeTextView.getText().toString()) * 1000;
                        int count = getContentCount(mWork4_content1TextView, mWork4_content2TextView, mWork4_content3TextView, mWork4_content4TextView, mWork4_content5TextView);

                        if (Work4_state == StateWaiting) {
                            long start = currentTimeMillis();
                            Log.d(TAG, "waiting -> working");
                            Work4_state = StateWorking;

                            tts_Work4.setSpeechRate(1.3f);
                            tts_Work4.speak("작업" + mWork4_nameTextView.getText().toString() + " 시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork4_stateTextView.setText(R.string.working_state_working);
                            mWork4.setBackgroundResource(R.color.green_50);

                            Work4_handler.postDelayed(runnablecode, time*count);
                            start_Work4time = currentTimeMillis();
                        }

                        else if(Work4_state == StatePause){
                            Work4_handler.postDelayed(runnablecode, time*count-pause_Work4time);
                            Log.d(TAG, "pause -> working");
                            Work4_state = StateWorking;
                            tts_Work4.setSpeechRate(1.3f);
                            tts_Work4.speak("작업" + mWork4_nameTextView.getText().toString() + " 재시작되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork4_stateTextView.setText(R.string.working_state_working);
                            mWork4.setBackgroundResource(R.color.green_50);

                        }

                        else if (Work4_state == StateWorking) {
                            pause_Work4time = currentTimeMillis()-start_Work4time;
                            if(Work4_handler != null) {Work4_handler.removeMessages(0);}
                            Log.d(TAG, "working -> pause");
                            Work4_state = StatePause;
                            tts_Work4.setSpeechRate(1.3f);
                            tts_Work4.speak("작업" + mWork4_nameTextView.getText().toString() + " 일시정지되었습니다", TextToSpeech.QUEUE_FLUSH, null);
                            mWork4_stateTextView.setText(R.string.working_state_pause);
                            mWork4.setBackgroundResource(R.color.red_50);
                        }
                    }

                    else{
                        Toast.makeText(getContext(), "작업 " + mWork4_nameTextView.getText().toString() +"의 시간을 입력하여 주십시오", Toast.LENGTH_LONG).show();
                    }
                }



                //else 추가










                mTextSpanBuffer.clear();
            }

            mPacketsCacheLastSize = packetsCacheSize;
        }

        updateBytesUI();
    }



    public int getContentCount(TextView mWork_content1TextView,TextView mWork_content2TextView, TextView mWork_content3TextView, TextView mWork_content4TextView, TextView mWork_content5TextView){
        int count = 0;
        if (!mWork_content1TextView.getText().toString().equals("")) {count++;}
        if (!mWork_content2TextView.getText().toString().equals("")) {count++;}
        if (!mWork_content3TextView.getText().toString().equals("")) {count++;}
        if (!mWork_content4TextView.getText().toString().equals("")) {count++;}
        if (!mWork_content5TextView.getText().toString().equals("")) {count++;}
        return count;
    }


    private String getItem(String protocol){
        switch (protocol){
            case "right\n":
                return "1";
            case "left\n":
                return "2";
            case "up\n":
                return "3";
            case "down1\n":
                return "4";
        }
        return "0";
    }


    private void onUartPacketText(UartPacket packet) {
        if (mIsEchoEnabled || packet.getMode() == UartPacket.TRANSFERMODE_RX) {
            final int color = colorForPacket(packet);
            final boolean isBold = isFontBoldForPacket(packet);
            final byte[] bytes = packet.getData();
            final String formattedData = mShowDataInHexFormat ? BleUtils.bytesToHex2(bytes) : BleUtils.bytesToText(bytes, true);
            addTextToSpanBuffer(mTextSpanBuffer, formattedData, color, isBold);
        }
    }

    private static SpannableString stringFromPacket(UartPacket packet, boolean useHexMode, int color, boolean isBold) {
        final byte[] bytes = packet.getData();
        final String formattedData = useHexMode ? BleUtils.bytesToHex2(bytes) : BleUtils.bytesToText(bytes, true);
        final SpannableString formattedString = new SpannableString(formattedData);
        formattedString.setSpan(new ForegroundColorSpan(color), 0, formattedString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (isBold) {
            formattedString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, formattedString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedString;
    }

    // endregion

    // region Mqtt UI
    private Runnable mMqttMenuItemAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            updateMqttStatus();
            mMqttMenuItemAnimationHandler.postDelayed(mMqttMenuItemAnimationRunnable, 500);
        }
    };
    private int mMqttMenuItemAnimationFrame = 0;

    @MainThread
    private void updateMqttStatus() {
        if (mMqttMenuItem == null) {
            return;      // Hack: Sometimes this could have not been initialized so we don't update icons
        }

        MqttManager.MqqtConnectionStatus status = mMqttManager.getClientStatus();

        if (status == MqttManager.MqqtConnectionStatus.CONNECTING) {
            final int[] kConnectingAnimationDrawableIds = {R.drawable.mqtt_connecting1, R.drawable.mqtt_connecting2, R.drawable.mqtt_connecting3};
            mMqttMenuItem.setIcon(kConnectingAnimationDrawableIds[mMqttMenuItemAnimationFrame]);
            mMqttMenuItemAnimationFrame = (mMqttMenuItemAnimationFrame + 1) % kConnectingAnimationDrawableIds.length;
        } else if (status == MqttManager.MqqtConnectionStatus.CONNECTED) {
            mMqttMenuItem.setIcon(R.drawable.mqtt_connected);
            mMqttMenuItemAnimationHandler.removeCallbacks(mMqttMenuItemAnimationRunnable);
        } else {
            mMqttMenuItem.setIcon(R.drawable.mqtt_disconnected);
            mMqttMenuItemAnimationHandler.removeCallbacks(mMqttMenuItemAnimationRunnable);
        }
    }

    // endregion

    // region Eol

    private String getEolCharacters() {
        switch (mEolCharactersId) {
            case 1:
                return "\r";
            case 2:
                return "\n\r";
            case 3:
                return "\r\n";
            default:
                return "\n";
        }
    }

    private int getEolCharactersStringId() {
        switch (mEolCharactersId) {
            case 1:
                return R.string.uart_eolmode_r;
            case 2:
                return R.string.uart_eolmode_nr;
            case 3:
                return R.string.uart_eolmode_rn;
            default:
                return R.string.uart_eolmode_n;
        }
    }

    // endregion

    // region Export

    private void export() {
        List<UartPacket> packets = mUartData.getPacketsCache();
        if (packets.isEmpty()) {
            showDialogWarningNoTextToExport();
        } else {
            // Export format dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.uart_export_format_subtitle);

            final String[] formats = {"txt", "csv", "json"};
            builder.setItems(formats, (dialog, which) -> {
                switch (which) {
                    case 0: { // txt
                        String result = UartDataExport.packetsAsText(packets, mShowDataInHexFormat);
                        exportText(result);
                        break;
                    }
                    case 1: { // csv
                        String result = UartDataExport.packetsAsCsv(packets, mShowDataInHexFormat);
                        exportText(result);
                        break;
                    }
                    case 2: { // json
                        String result = UartDataExport.packetsAsJson(packets, mShowDataInHexFormat);
                        exportText(result);
                        break;
                    }
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void exportText(@Nullable String text) {
        if (text != null && !text.isEmpty()) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.uart_export_format_title)));
        } else {
            showDialogWarningNoTextToExport();
        }
    }


    private void showDialogWarningNoTextToExport() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        //builder.setTitle(R.string.);
        builder.setMessage(R.string.uart_export_nodata);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    // endregion

    // region UartPacketManagerBase.Listener

    @Override
    public void onUartPacket(UartPacket packet) {
        updateBytesUI();
    }

    // endregion

    // region MqttManagerListener

    @MainThread
    @Override
    public void onMqttConnected() {
        updateMqttStatus();
    }

    @MainThread
    @Override
    public void onMqttDisconnected() {
        updateMqttStatus();
    }

    // endregion

    // region Buffer Adapter

    class TimestampItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        // ViewHolder
        class ItemViewHolder extends RecyclerView.ViewHolder {
            ViewGroup mainViewGroup;
            TextView timestampTextView;
            TextView dataTextView;

            ItemViewHolder(View view) {
                super(view);

                mainViewGroup = view.findViewById(R.id.mainViewGroup);
                timestampTextView = view.findViewById(R.id.timestampTextView);
                dataTextView = view.findViewById(R.id.dataTextView);
            }
        }

        // Data
        private Context mContext;
        private boolean mIsEchoEnabled;
        private boolean mShowDataInHexFormat;
        private UartPacketManagerBase mUartData;
        private List<UartPacket> mTableCachedDataBuffer;
        private SimpleDateFormat mDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        TimestampItemAdapter(@NonNull Context context) {
            super();
            mContext = context;
        }

        void setUartData(@Nullable UartPacketManagerBase uartData) {
            mUartData = uartData;
            notifyDataSetChanged();
        }

        int getCachedDataBufferSize() {
            return mTableCachedDataBuffer != null ? mTableCachedDataBuffer.size() : 0;
        }

        void setEchoEnabled(boolean isEchoEnabled) {
            mIsEchoEnabled = isEchoEnabled;
            notifyDataSetChanged();
        }

        void setShowDataInHexFormat(boolean showDataInHexFormat) {
            mShowDataInHexFormat = showDataInHexFormat;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_uart_packetitem, parent, false);
            return new TimestampItemAdapter.ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;

            UartPacket packet = mTableCachedDataBuffer.get(position);
            final String currentDateTimeString = mDateFormat.format(new Date(packet.getTimestamp()));//DateFormat.getTimeInstance().format(new Date(packet.getTimestamp()));
            final String modeString = mContext.getString(packet.getMode() == UartPacket.TRANSFERMODE_RX ? R.string.uart_timestamp_direction_rx : R.string.uart_timestamp_direction_tx);
            final int color = colorForPacket(packet);
            final boolean isBold = isFontBoldForPacket(packet);

            itemViewHolder.timestampTextView.setText(String.format("%s %s", currentDateTimeString, modeString));

            SpannableString text = stringFromPacket(packet, mShowDataInHexFormat, color, isBold);
            itemViewHolder.dataTextView.setText(text);

            itemViewHolder.mainViewGroup.setBackgroundColor(position % 2 == 0 ? Color.WHITE : 0xeeeeee);
        }

        @Override
        public int getItemCount() {
            if (mUartData == null) {
                return 0;
            }

            if (mIsEchoEnabled) {
                mTableCachedDataBuffer = mUartData.getPacketsCache();
            } else {
                if (mTableCachedDataBuffer == null) {
                    mTableCachedDataBuffer = new ArrayList<>();
                } else {
                    mTableCachedDataBuffer.clear();
                }

                List<UartPacket> packets = mUartData.getPacketsCache();
                for (int i = 0; i < packets.size(); i++) {
                    UartPacket packet = packets.get(i);
                    if (packet != null && packet.getMode() == UartPacket.TRANSFERMODE_RX) {     // packet != null because crash found in google logs
                        mTableCachedDataBuffer.add(packet);
                    }
                }
            }

            return mTableCachedDataBuffer.size();
        }
    }

    // endregion

}

