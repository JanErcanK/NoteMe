package de.noteme.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.*;
import android.speech.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

public final class CaptureActivity extends Activity {
    static final String NOTE_ID="note_id";
    private Notes notes;
    private long id;
    private EditText editor;
    private TextView mic, feedback;
    private SpeechRecognizer recognizer;
    private String partial="";
    private boolean listening, stopping;
    private int insertStart,insertEnd;
    private long session;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        notes=new Notes(this);
        id=state==null?getIntent().getLongExtra(NOTE_ID,0):state.getLong(NOTE_ID);
        Notes.Entry note=id==0?null:notes.get(id);
        if(id!=0 && note==null) { finish(); return; }

        LinearLayout card=Ui.column(this); Ui.pad(card,20,20); card.setBackground(Ui.shape(this,Ui.PAPER,24,Ui.RULE));
        card.addView(Ui.small(this,"NOTEME  /  NOTIZBUCH")); Ui.gap(card,8);
        card.addView(Ui.title(this,note==null?"Schnellnotiz":"Notiz bearbeiten",30)); Ui.gap(card,12);
        Ui.line(card); Ui.gap(card,6);
        editor=new EditText(this); editor.setTextSize(18); editor.setTextColor(Ui.INK); editor.setHintTextColor(Ui.MUTED);
        editor.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); editor.setBackgroundColor(0);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setGravity(Gravity.TOP|Gravity.START); editor.setMinLines(2); editor.setMaxLines(5);
        editor.setHint("Was möchtest du festhalten?"); Ui.pad(editor,0,8);
        if(note!=null) editor.setText(note.body);
        editor.setSelection(editor.length()); card.addView(editor);
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                id=notes.save(id,s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });
        feedback=Ui.label(this,"",13,Ui.MUTED); feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        feedback.setMaxLines(3); card.addView(feedback); Ui.gap(card,10);
        LinearLayout actions=Ui.row(this);
        mic=Ui.button(this,"Diktieren",false,this::dictate); Ui.flex(actions,mic);
        actions.addView(new Space(this),Ui.dp(this,10),1);
        Ui.flex(actions,Ui.button(this,"Fertig",true,this::finish)); card.addView(actions);

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false); scroll.addView(card); setContentView(scroll);
        Window window=getWindow();
        int width=Math.min(Ui.dp(this,460),getResources().getDisplayMetrics().widthPixels-Ui.dp(this,28));
        window.setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);
        editor.requestFocus();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if(focus && editor!=null && editor.hasFocus() && !listening)
            editor.post(()->getSystemService(InputMethodManager.class).showSoftInput(editor,InputMethodManager.SHOW_IMPLICIT));
    }
    @Override protected void onResume() { super.onResume(); BearService.capturing(true); }
    @Override protected void onPause() { endSession(true); BearService.capturing(false); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putLong(NOTE_ID,id); }
    private void dictate() {
        if(listening) {
            if(!stopping) { stopping=true; recognizer.stopListening(); feedback.setText("Diktat wird übernommen …"); }
            return;
        }
        boolean local=Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
        if(!local && !SpeechRecognizer.isRecognitionAvailable(this)) {
            feedback.setText("Auf diesem Gerät ist kein Sprachdienst verfügbar."); return;
        }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7); return;
        }
        insertStart=Math.max(0,editor.getSelectionStart()); insertEnd=Math.max(insertStart,editor.getSelectionEnd());
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(editor.getWindowToken(),0);
        editor.setEnabled(false); listening=true; stopping=false; partial="";
        mic.setText("Stopp"); feedback.setText("Mikrofon wird gestartet …");
        long token=++session;
        try {
            recognizer=local?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) { if(token==session) feedback.setText("Ich höre zu …"); }
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float v) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() { if(token==session) feedback.setText("Diktat wird übernommen …"); }
                public void onError(int code) {
                    if(token!=session) return;
                    endSession(true);
                    String message;
                    switch(code) {
                        case SpeechRecognizer.ERROR_NO_MATCH: case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message="Keine Sprache erkannt. Versuche es erneut."; break;
                        case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE: case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                            message="Für dieses Diktat fehlt ein Sprachpaket. Prüfe die Spracheinstellungen."; break;
                        case SpeechRecognizer.ERROR_NETWORK: case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            message="Der Sprachdienst ist offline. Bitte erneut versuchen."; break;
                        default: message="Diktat unterbrochen. Du kannst weiterschreiben oder erneut diktieren.";
                    }
                    feedback.setText(message);
                }
                public void onResults(Bundle b) {
                    if(token!=session) return;
                    String result=first(b); if(!result.isEmpty()) partial=result;
                    boolean received=!partial.isEmpty(); endSession(true);
                    feedback.setText(received?"Diktat gespeichert.":"Keine Sprache erkannt.");
                }
                public void onPartialResults(Bundle b) {
                    if(token!=session) return;
                    String text=first(b); if(!text.isEmpty()) { partial=text; feedback.setText(text); }
                }
                public void onEvent(int type,Bundle b) {}
            });
            Intent voice=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag());
            voice.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
            recognizer.startListening(voice);
        } catch(RuntimeException e) { endSession(false); feedback.setText("Der Sprachdienst konnte nicht starten."); }
    }
    private String first(Bundle b) {
        ArrayList<String> list=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return list==null||list.isEmpty()?"":list.get(0).trim();
    }
    private void endSession(boolean keep) {
        if(!listening) return;
        session++; listening=false; stopping=false;
        if(recognizer!=null) { recognizer.cancel(); recognizer.destroy(); recognizer=null; }
        editor.setEnabled(true); mic.setText("Diktieren"); feedback.setText("");
        if(keep && !partial.isEmpty()) {
            Editable text=editor.getText();
            String prefix=insertStart>0&&!Character.isWhitespace(text.charAt(insertStart-1))?" ":"";
            String suffix=insertEnd<text.length()&&!Character.isWhitespace(text.charAt(insertEnd))?" ":"";
            String phrase=prefix+partial+suffix;
            text.replace(insertStart,insertEnd,phrase); editor.setSelection(insertStart+phrase.length());
        }
        partial="";
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request!=7) return;
        if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) dictate();
        else feedback.setText("Zum Diktieren braucht NoteMe Mikrofonzugriff.");
    }
    @Override protected void onDestroy() {
        endSession(true);
        if(isFinishing()) notes.discardEmpty(id);
        notes.close(); super.onDestroy();
    }
}

