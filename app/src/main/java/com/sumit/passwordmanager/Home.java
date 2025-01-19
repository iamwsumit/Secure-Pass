package com.sumit.passwordmanager;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ferfalk.simplesearchview.SimpleSearchView;
import com.github.ivbaranov.mli.MaterialLetterIcon;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Home extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<Model> models;
    private SimpleSearchView searchView;
    private String currentSearch;

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen())
            searchView.closeSearch(true);
        else
            finishAffinity();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        populateRecyclerView();
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        Utils.setStatusBarColor(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        FloatingActionButton fab = findViewById(R.id.floating_action_button);
        fab.setOnClickListener(view -> startActivity(new Intent(Home.this, Password.class)));

        this.recyclerView = findViewById(R.id.recyclerview);
        AppBarLayout appBarLayout = findViewById(R.id.appBar);

        View.inflate(this, Utils.isDarkTheme(this) ? R.layout.dark_appbar : R.layout.light_appbar,
                findViewById(R.id.toolbar_container));
        View.inflate(this, R.layout.searchview, findViewById(R.id.toolbar_container));

        this.searchView = findViewById(R.id.toolbar_container).findViewById(R.id.searchView);
        this.searchView.setOnQueryTextListener(new SimpleSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(@NotNull String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(@NotNull String s) {
                currentSearch = s;
                search();
                return false;
            }

            @Override
            public boolean onQueryTextCleared() {
                return false;
            }
        });

        this.searchView.setOnSearchViewListener(new SimpleSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {

            }

            @Override
            public void onSearchViewClosed() {
                if (!currentSearch.isEmpty()) {
                    currentSearch = "";
                    search();
                }
            }

            @Override
            public void onSearchViewShownAnimation() {

            }

            @Override
            public void onSearchViewClosedAnimation() {

            }
        });

        BottomNavigationView navigationView = findViewById(R.id.bottom_navigation);

        ((MaterialToolbar) appBarLayout.findViewById(R.id.topAppBar)).setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.theme) {
                Utils.toggleTheme(Home.this);
                navigationView.setSelectedItemId(R.id.vault);
            } else if (item.getItemId() == R.id.search) {
                this.searchView.showSearch(true);
            }
            return true;
        });

        navigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.vault) {
                findViewById(R.id.vaultContainer).setVisibility(View.VISIBLE);
                findViewById(R.id.generator).setVisibility(View.GONE);
            } else if (item.getItemId() == R.id.generator) {
                findViewById(R.id.vaultContainer).setVisibility(View.GONE);
                findViewById(R.id.generator).setVisibility(View.VISIBLE);
            } else return false;
            return true;
        });
        navigationView.setSelectedItemId(R.id.vault);

        populateRecyclerView();
        initializePasswordGenerator();
    }

    public void search() {
        if (models.isEmpty() || adapter == null)
            return;
        List<Model> tempList = new ArrayList<>();
        if (currentSearch.isEmpty())
            tempList = models;
        else {
            for (Model model : models) {
                if (model.getTitle().toLowerCase().contains(currentSearch.toLowerCase())
                        || model.getUsername().toLowerCase().contains(currentSearch.toLowerCase())
                        || currentSearch.toLowerCase().contains(model.getTitle().toLowerCase()))
                    tempList.add(model);
            }
        }
        adapter.setModels(tempList);
    }

    private ListAdapter adapter;

    private void populateRecyclerView() {
        try {
            DatabaseHelper db = new DatabaseHelper(this);
            db.fetch(models -> this.models = models);
            db.close();
            if (models.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                findViewById(R.id.notFound).setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                findViewById(R.id.notFound).setVisibility(View.GONE);
                if (adapter == null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                    adapter = new ListAdapter();
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.setModels(models);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong " + e.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
        }
    }

    private static final String CAPITAL = "QWERTYUIOPASDFGHJKLZXCVBNM";
    private static final String SMALL = "qwertyuiopasdfghjklzxcvbnm";
    private static final String NUMBERS = "1234567890";
    private static final String SPECIAL = "!@#$%^&*()-_=+[{]}\\|;:<>/?~";

    private TextView generatedPassword;
    private TextView passLength;
    private SeekBar seekbar;
    private Switch capital;
    private Switch small;
    private Switch numbers;
    private Switch special;
    private Switch duplicate;

    private void generatePassword() {
        new Thread(() -> {
            StringBuilder builder = new StringBuilder();
            if (capital.isChecked())
                builder.append(CAPITAL);
            if (small.isChecked())
                builder.append(SMALL);
            if (numbers.isChecked())
                builder.append(NUMBERS);
            if (special.isChecked())
                builder.append(SPECIAL);
            String gen = "";
            if (!builder.toString().isEmpty()) {
                final List<String> list = Arrays.asList(builder.toString().split(""));
                Collections.shuffle(list);
                int progress = seekbar.getProgress();

                while (gen.length() < progress && (!duplicate.isChecked() || gen.length() < list.size())) {
                    String chars = list.get(new Random().nextInt(list.size()));
                    if (gen.contains(chars) && duplicate.isChecked())
                        continue;
                    gen += chars;
                }

            } else {
                runOnUiThread(() -> Toast.makeText(Home.this, "Please enable at least one character set", Toast.LENGTH_LONG).show());
                return;
            }
            String finalGen = gen;
            runOnUiThread(() -> generatedPassword.setText(finalGen));
        }).start();

    }

    private void initializePasswordGenerator() {
        this.generatedPassword = findViewById(R.id.passGen);
        this.passLength = findViewById(R.id.passLength);
        this.seekbar = findViewById(R.id.seekbar);
        this.capital = findViewById(R.id.capital);
        this.special = findViewById(R.id.special);
        this.numbers = findViewById(R.id.numbers);
        this.small = findViewById(R.id.small);
        this.duplicate = findViewById(R.id.duplicate);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Save Password")
                .setMessage("Do you want to save this password?")
                .setNegativeButton("No", (dialogInterface, i) -> {
                    ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    manager.setPrimaryClip(ClipData.newPlainText("Password", generatedPassword.getText().toString()));
                    Toast.makeText(Home.this, "Copied", Toast.LENGTH_SHORT).show();
                }).setPositiveButton("Yes", (dialogInterface, i) -> {
                    startActivity(new Intent(Home.this, Password.class)
                            .putExtra("generated_pass", generatedPassword.getText().toString()));
                }).create();

        View.OnClickListener clickListener = (v) -> {
            if (v.getId() == R.id.refresh) {
                generatePassword();
            } else if (v.getId() == R.id.copy) {
                dialog.show();
            }
        };

        findViewById(R.id.refresh).setOnClickListener(clickListener);
        findViewById(R.id.copy).setOnClickListener(clickListener);

        this.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                passLength.setText("Password Length: " + i);
                generatePassword();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final CompoundButton.OnCheckedChangeListener listener = (compoundButton, b) -> {
            seekbar.setEnabled(capital.isChecked() | small.isChecked() | numbers.isChecked() | special.isChecked());
            generatePassword();
        };

        this.capital.setOnCheckedChangeListener(listener);
        this.small.setOnCheckedChangeListener(listener);
        this.numbers.setOnCheckedChangeListener(listener);
        this.special.setOnCheckedChangeListener(listener);
        this.duplicate.setOnCheckedChangeListener(listener);
        generatePassword();
    }

    public final class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

        public List<Model> models;

        final int[] profilePicBackgroundColors = new int[]{
                getResources().getColor(R.color.material_blue_500),      // Blue
                getResources().getColor(R.color.material_red_500),       // Red
                getResources().getColor(R.color.material_green_500),     // Green
                getResources().getColor(R.color.material_yellow_500),    // Yellow
                getResources().getColor(R.color.material_purple_500),    // Purple
                getResources().getColor(R.color.material_indigo_500),    // Indigo
                getResources().getColor(R.color.material_orange_500),    // Orange
                getResources().getColor(R.color.material_teal_500),      // Teal
                getResources().getColor(R.color.material_brown_500),     // Brown
                getResources().getColor(R.color.material_cyan_500)       // Cyan
        };


        public ListAdapter() {
            this.models = Home.this.models;
        }

        public void setModels(List<Model> models) {
            this.models = models;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(Home.this)
                    .inflate(R.layout.password_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Model model = models.get(position);
            final String username = model.getUsername();
            final String time = new SimpleDateFormat("DD MMMM YYYY")
                    .format(new Date(model.getCreatedTime()));
            holder.titleTextView.setText(model.getTitle());
            holder.subtitleTextView.setText(username.isEmpty() ? time : username);
            holder.letterIcon.setLetter(model.getTitle().substring(0, 1).toUpperCase());
        }

        @Override
        public int getItemCount() {
            return this.models.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            TextView subtitleTextView;
            MaterialLetterIcon letterIcon;

            public ViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.title);
                subtitleTextView = itemView.findViewById(R.id.subtitle);
                letterIcon = itemView.findViewById(R.id.imageView);

                letterIcon.setShapeColor(profilePicBackgroundColors[new Random()
                        .nextInt(profilePicBackgroundColors.length)]);

                itemView.setOnClickListener(view -> {
                    Password.model = ListAdapter.this.models.get(getAdapterPosition());
                    startActivity(new Intent(Home.this, Password.class));
                });
            }
        }
    }
}