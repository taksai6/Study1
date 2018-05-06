package jp.sai.study;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private int mGenre = 0;
    private DatabaseReference mDatabaseReference, mGenreRef, mUserRef;

    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;

    private FirebaseUser mUser;
    ArrayList<String> mfavList = new ArrayList<>();
    private boolean mfavflag = false;


    private ChildEventListener mEventListener = new ChildEventListener() {

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {


            if (mfavflag == false) {
                addQuestionList(dataSnapshot);

            } else if (mfavList.contains(dataSnapshot.getRef().toString())) {
                addQuestionList(dataSnapshot);

            }

        }


        private void addQuestionList(DataSnapshot dataSnapshot) {    // onChildAdded内処理を分離

            int genre = Integer.parseInt(dataSnapshot.getRef().getParent().getKey());   // ジャンルもfirebase上のデータを使用


            HashMap map = (HashMap) dataSnapshot.getValue();
            String title = (String) map.get("title");
            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");
            String imageString = (String) map.get("image");

            Bitmap image = null;

            byte[] bytes;

            if (imageString != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                bytes = Base64.decode(imageString, Base64.DEFAULT);

            } else {
                bytes = new byte[0];

            }


            ArrayList<Answer> answerArrayList = new ArrayList<Answer>();

            HashMap answerMap = (HashMap) map.get("answers");

            if (answerMap != null) {

                for (Object key : answerMap.keySet()) {
                    HashMap temp = (HashMap) answerMap.get((String) key);

                    String answerBody = (String) temp.get("body");
                    String answerName = (String) temp.get("name");
                    String answerUid = (String) temp.get("uid");

                    Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);

                    answerArrayList.add(answer);

                }

            }


            Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), genre, bytes, answerArrayList);
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();

        }


        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            HashMap map = (HashMap) dataSnapshot.getValue();


            // 変更があったQuestionを探す

            for (Question question : mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ

                    question.getAnswers().clear();

                    HashMap answerMap = (HashMap) map.get("answers");

                    if (answerMap != null) {

                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);

                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");

                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);

                            question.getAnswers().add(answer);

                        }

                    }

                    mAdapter.notifyDataSetChanged();

                }

            }

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

        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ

                if (mGenre == 0) {

                    Snackbar.make(view, "科目を選択して下さい", Snackbar.LENGTH_LONG).show();

                    return;

                }

                // Firebase ログイン済みのユーザーを収録する

                mUser = FirebaseAuth.getInstance().getCurrentUser();


                if (mUser == null) {
                    // ログインしていなければログイン画面に遷移させる

                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);

                    startActivity(intent);

                } else {
                    // ジャンルを渡して質問作成画面を起動する

                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);

                    intent.putExtra("genre", mGenre);

                    startActivity(intent);

                }

            }

        });


        // ナビゲーションドロワーの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);

        drawer.addDrawerListener(toggle);

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem item) {


                // Firebase ログイン済みのユーザーを収録する

                mUser = FirebaseAuth.getInstance().getCurrentUser();
                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

                if (mUser == null) {
                    Menu menu = navigationView.getMenu();
                    MenuItem menuItem1 = menu.findItem(R.id.nav_favorite);
                    menuItem1.setVisible(false);

                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);

                    startActivity(intent);

                    return true;

                } else {
                    // ログインしていればReferenceを取得する
                    mDatabaseReference = FirebaseDatabase.getInstance().getReference();
                    mUserRef = mDatabaseReference.child(Const.UsersPATH).child(mUser.getUid());

                }


                int id = item.getItemId();

                if (mfavflag == true) mfavflag = false;


                if (id == R.id.nav_japanese) {
                    mToolbar.setTitle("国語");
                    mGenre = 1;

                } else if (id == R.id.nav_math) {
                    mToolbar.setTitle("数学");
                    mGenre = 2;

                } else if (id == R.id.nav_english) {
                    mToolbar.setTitle("英語");
                    mGenre = 3;

                } else if (id == R.id.nav_jhistory) {
                    mToolbar.setTitle("日本史");
                    mGenre = 4;

                } else if (id == R.id.nav_whistory) {
                    mToolbar.setTitle("世界史");
                    mGenre = 5;
                } else if (id == R.id.nav_geography) {
                    mToolbar.setTitle("地理");
                    mGenre =6;
                } else if (id == R.id.nav_social) {
                    mToolbar.setTitle("政治経済/倫理/現代社会");
                    mGenre =7;
                } else if (id == R.id.nav_physics) {
                    mToolbar.setTitle("物理");
                    mGenre = 8;
                } else if (id == R.id.nav_chemistory) {
                    mToolbar.setTitle("化学");
                    mGenre = 9;
                } else if (id == R.id.nav_biology) {
                    mToolbar.setTitle("生物");
                    mGenre = 10;
                } else if (id == R.id.nav_earth) {
                    mToolbar.setTitle("地学");
                    mGenre = 11;
                } else if (id == R.id.nav_favorite) {
                    mToolbar.setTitle("[お気に入り]");
                    mfavflag = true;

                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);


                // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す

                mQuestionArrayList.clear();

                mAdapter.setQuestionArrayList(mQuestionArrayList);
                mListView.setAdapter(mAdapter);

                // 選択したジャンルにリスナーを登録する
                if (mGenreRef != null) {
                    mGenreRef.removeEventListener(mEventListener);

                }


                if (mfavflag == true) {
                    mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot ds) {
                            if ((ArrayList<String>) ds.child("fav").getValue() != null) {
                                mfavList = (ArrayList<String>) ds.child("fav").getValue();

                                for (int i = 1; i < 11; i++) {
                                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(i));
                                    mGenreRef.addChildEventListener(mEventListener);

                                }

                                mGenre = 0;

                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });

                } else {
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                    mGenreRef.addChildEventListener(mEventListener);

                }

                return true;
            }

        });

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();

        mAdapter.notifyDataSetChanged();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);

            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}