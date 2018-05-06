package jp.sai.study;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class QuestionDetailActivity extends AppCompatActivity {
    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;
    private Button favoriteButton;
    ArrayList<String> favList = new ArrayList<>();
    private String mCurrentQuestionId;

    private DatabaseReference mdbRef, mAnswerRef, mQuestionRef, mUserRef;
    private boolean favRegisteredFlag;

    private ChildEventListener mEventListener = new ChildEventListener() {

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
            String answerUid = dataSnapshot.getKey();

            for (Answer answer : mQuestion.getAnswers()) {

                if (answerUid.equals(answer.getAnswerUid())) {

                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        favoriteButton = (Button) findViewById(R.id.button);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");
        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // ログイン済みのユーザーを収録する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // Questionを渡して回答作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);

                }

            }

        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            favoriteButton.setVisibility(View.INVISIBLE);
        } else {

            favoriteButton.setVisibility(View.VISIBLE);
            mdbRef = FirebaseDatabase.getInstance().getReference();
            mUserRef = mdbRef.child(Const.UsersPATH).child(user.getUid());
            mQuestionRef = mdbRef.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid());
            mAnswerRef = mQuestionRef.child(Const.AnswersPATH);
            mCurrentQuestionId = mQuestionRef.getRef().toString();

            FavRegisteredCheck();

            if (favRegisteredFlag) {
                favoriteButton.setText(R.string.fav_registered);
            }

        }

    }
    private void FavRegisteredCheck() {

        favRegisteredFlag = false;
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot ds) {

                if ((ArrayList<String>) ds.child("fav").getValue() != null) {
                    favList = (ArrayList<String>) ds.child("fav").getValue();
                    if (favList.contains(mCurrentQuestionId)) {

                        favoriteButton.setText(R.string.fav_registered);
                        favRegisteredFlag = true;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }


    public void favorite(View v) {
        if (favRegisteredFlag) {
            favList.remove(mCurrentQuestionId);
            favoriteButton.setText(R.string.fav_unregistered);
            favRegisteredFlag = false;


        } else {
            favList.add(mCurrentQuestionId);
            favoriteButton.setText(R.string.fav_registered);
            favRegisteredFlag = true;

        }
        mUserRef.child("fav").setValue(favList);
    }

}