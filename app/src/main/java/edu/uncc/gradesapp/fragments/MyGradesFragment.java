package edu.uncc.gradesapp.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import edu.uncc.gradesapp.R;
import edu.uncc.gradesapp.databinding.FragmentMyGradesBinding;
import edu.uncc.gradesapp.databinding.GradeRowItemBinding;
import edu.uncc.gradesapp.models.Grade;

public class MyGradesFragment extends Fragment {

    public static final String TAG = "debug";

    public MyGradesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.action_add){
            mListener.gotoAddGrade();
            return true;
        } else if(item.getItemId() == R.id.action_logout) {
            mListener.logout();
            return true;
        } else if(item.getItemId() == R.id.action_reviews){
            mListener.gotoViewReviews();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    FragmentMyGradesBinding binding;
    ArrayList<Grade> mGrades = new ArrayList<>();
    GradesAdapter mAdapter;
    RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyGradesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("My Grades");

        mRecyclerView = binding.recyclerView;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new GradesAdapter();
        mRecyclerView.setAdapter(mAdapter);

        this.FetchGradesForCurrentUser();
    }


    ListenerRegistration listenerRegistration;
    private void FetchGradesForCurrentUser() {

        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser(); // get current user
        if(mUser != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance(); // get instance of firestore

            listenerRegistration = db.collection("gradesCollection").addSnapshotListener(new EventListener<QuerySnapshot>() { // listen for changes in the collection
                @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    Log.d(TAG, "Total Courses: " + value.size());

                    if (error != null) {
                        Log.d(TAG, "Failed to Listen for 'gradesCollection' Updates.", error);
                        return;
                    }

                    mGrades.clear();
                    double totalCreditHours = 0;
                    double totalGradePoints = 0;

                    for (QueryDocumentSnapshot doc : value) { // iterate over all documents in the collection
                        //Log.d(TAG, doc.getId() + " => " + doc.getData());
                        String createdByUid = (String) doc.get("CreatedByUid"); // get the createdByUid field

                        if (createdByUid != null && createdByUid.equals(mUser.getUid())) { // check if the createdByUid field matches the current user's uid
                            HashMap<String, Object> gradesHashMap = (HashMap<String, Object>) doc.getData(); // get the data of the document - (.getData) returns a Map<String, Object>
                            Grade grade = new Grade(gradesHashMap); // create a new Grade object from the data
                            //Log.d(TAG, "Grade Created: " + grade.toString());
                            mGrades.add(grade); // add the grade to the list to be displayed by the adapter

                            totalCreditHours += grade.getCreditHours();
                            totalGradePoints += (grade.getCreditHours() * grade.getNumericGrade());
                        }
                    }

                    Log.d(TAG, "Total Courses for Current User: " + mGrades.size());
                    mAdapter.notifyDataSetChanged();

                    if (totalCreditHours == 0) {
                        binding.textViewHours.setText("Hours: " + 0.0);
                        binding.textViewGPA.setText("GPA: " + 4.0);
                    } else {
                        String TotalHours = String.format("%.1f", totalCreditHours);
                        String GPA = String.format("%.2f", totalGradePoints / totalCreditHours);
                        binding.textViewHours.setText("Hours: " + TotalHours);
                        binding.textViewGPA.setText("GPA: " + GPA);
                    }
                }
            });
        }
    }

    class GradesAdapter extends RecyclerView.Adapter<GradesAdapter.GradeViewHolder> {

        @NonNull
        @Override
        public GradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            GradeRowItemBinding binding = GradeRowItemBinding.inflate(getLayoutInflater(), parent, false);
            return new GradeViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull GradeViewHolder holder, int position) {
            Grade grade = mGrades.get(position);
            holder.setupUI(grade);
        }

        @Override
        public int getItemCount() {
            return mGrades.size();
        }

        class GradeViewHolder extends RecyclerView.ViewHolder {

            GradeRowItemBinding mBinding;
            Grade mGrade;

            public GradeViewHolder(GradeRowItemBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }

            @SuppressLint("SetTextI18n")
            public void setupUI(Grade grade) {
                mGrade = grade;
                mBinding.textViewCourseNumber.setText(mGrade.getCourseNumber());
                mBinding.textViewCourseName.setText(mGrade.getCourseName());
                mBinding.textViewSemester.setText(mGrade.getSemesterNameYear());
                mBinding.textViewLetterGrade.setText(mGrade.getLetterGrade());
                mBinding.textViewCreditHours.setText(mGrade.getCreditHours() + " Credit Hours");

                mBinding.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Delete Grade?").setMessage("Are you sure you want to delete this grade?");
                        builder.setPositiveButton("Yes", (dialog, which) -> {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            DocumentReference docRef = db.collection("gradesCollection").document(mGrade.getDocId());
                            docRef.delete().addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "DocumentSnapshot Successfully Deleted!");
                            }).addOnFailureListener(e -> {
                                Log.w(TAG, "Error Deleting Document", e);
                            });
                        }).setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss();
                        });
                        builder.create().show();
                    }
                });
            }
        }
    }

    MyGradesListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        //try catch block
        try {
            mListener = (MyGradesListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " must implement MyGradesListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
    }

    public interface MyGradesListener {
        void gotoAddGrade();
        void logout();
        void gotoViewReviews();
    }
}