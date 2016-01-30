package cz.msebera.captcha;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.urbandroid.sleep.captcha.CaptchaSupport;
import com.urbandroid.sleep.captcha.CaptchaSupportFactory;
import com.urbandroid.sleep.captcha.RemainingTimeListener;

import java.util.List;

public final class CaptchaActivity extends Activity {

    private CaptchaSupport captchaSupport;
    private Toast currentToast;
    private SpeechRecognizer speech;
    private final Handler handler = new Handler();
    private int currentOrientation = -1;
    private int difficultyCoefficient = 20;
    private int count = 0;
    private boolean recognitionStarted = false;
    private ProgressBar voiceLevel;
    private TextView score, scoreLabel;
    public static final String TAG = "F*ckaptcha";

    private final RemainingTimeListener remainingTimeListener = new RemainingTimeListener() {
        @Override
        public void timeRemain(int seconds, int aliveTimeout) {
            if (currentToast != null) {
                currentToast.cancel();
            }
            currentToast = Toast.makeText(CaptchaActivity.this, getString(R.string.timeout_toast, seconds, aliveTimeout), Toast.LENGTH_LONG);
            currentToast.show();
        }
    };

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            voiceLevel.setProgress((int) ((rmsdB + 120) / 1.8));
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
            startVoiceRecognition();
        }

        @Override
        public void onError(int error) {
            Log.d(TAG, String.format("onError %d", error));
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    scoreLabel.setText("Audio is Fucked!");
                    stopFuckingAround();
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    scoreLabel.setText("Client is Fucked!");
                    stopFuckingAround();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    scoreLabel.setText("Permissions are Fucked Up!");
                    stopFuckingAround();
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    scoreLabel.setText("Network is Fucked Up!");
                    stopFuckingAround();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    scoreLabel.setText("Network timeout, Fuck!");
                    startVoiceRecognition();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    scoreLabel.setText("Stop Mumbling!");
                    startVoiceRecognition();
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    scoreLabel.setText("Recognizer busy, OMG!");
                    startVoiceRecognition();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    scoreLabel.setText("Server error, WTF?");
                    stopFuckingAround();
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    scoreLabel.setText("SWEAR GOD DAMMIT!");
                    startVoiceRecognition();
                    break;
                default:
                    startVoiceRecognition();
                    break;
            }
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults");
            workStringList(results);
            startVoiceRecognition();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
            workStringList(partialResults);
            startVoiceRecognition();
        }

        private void workStringList(Bundle results) {
            List<String> resultStrings = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (results == null || resultStrings == null) {
                startVoiceRecognition();
                return;
            }
            for (String s : resultStrings) {
                captchaSupport.alive();
                Log.d(TAG, String.format("%s", s));
                for (String word : s.split(" ")) {
                    Log.d(TAG, String.format("%s", word));
                    if (word.contains("*")) {
                        count += difficultyCoefficient;
                        checkFinished();
                    }
                }
            }
            score.setText(String.format("%d%%", count));
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, String.format("onEvent %d", eventType));
            Log.d(TAG, params.toString());
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        captchaSupport = CaptchaSupportFactory.create(this);
        captchaSupport.setRemainingTimeListener(remainingTimeListener);

        ((TextView) findViewById(R.id.difficulty)).setText(getString(R.string.difficulty, captchaSupport.getDifficulty()));
        setDifficulty();
        score = (TextView) findViewById(R.id.score);
        scoreLabel = (TextView) findViewById(R.id.score_label);
        voiceLevel = (ProgressBar) findViewById(R.id.voiceLevel);
    }

    private void setDifficulty() {
        final int[] diffMap = new int[]{
                100,
                25,
                20,
                15,
                10,
                5
        };
        difficultyCoefficient = diffMap[captchaSupport.getDifficulty()];
    }

    private void checkFinished() {
        if (count >= 100) {
            captchaSupport.solved();
            finish();
        }
    }

    private void killVoiceRecognition() {
        if (speech != null) {
            Log.d(TAG, "killVoiceRecognition");
            speech.stopListening();
            speech.destroy();
            speech = null;
            recognitionStarted = true;
        }
    }

    private synchronized void startVoiceRecognition() {
        Log.d(TAG, "startVoiceRecognition");
        if (recognitionStarted) {
            return;
        }
        if (speech == null) {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(recognitionListener);
        }
        recognitionStarted = true;
        Intent startIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        startIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300);
        startIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        if (Build.VERSION.SDK_INT >= 23) {
            startIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        }
        startIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        startIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
        speech.startListening(startIntent);
    }

    private void stopFuckingAround() {
        voiceLevel.setProgress(0);
        killVoiceRecognition();
        handler.removeCallbacksAndMessages(null);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    private void fuckUpScreenOrientation() {
        if (currentOrientation < 0) return;
        if (currentOrientation == -1) {
            currentOrientation = getWindowManager().getDefaultDisplay().getOrientation();
        }
        switch (currentOrientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            default:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
        setRequestedOrientation(currentOrientation);
        handler.removeCallbacksAndMessages(true);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fuckUpScreenOrientation();
            }
        }, 1000);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        captchaSupport = CaptchaSupportFactory
                .create(this, intent)
                .setRemainingTimeListener(remainingTimeListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopFuckingAround();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopFuckingAround();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startVoiceRecognition();
        fuckUpScreenOrientation();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopFuckingAround();
        captchaSupport.unsolved();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        captchaSupport.alive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFuckingAround();
        captchaSupport.destroy();
    }
}
