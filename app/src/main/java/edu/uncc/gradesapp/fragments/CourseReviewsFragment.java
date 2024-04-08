package edu.uncc.gradesapp.fragments;

import android.annotation.SuppressLint;
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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.uncc.gradesapp.R;
import edu.uncc.gradesapp.databinding.CourseReviewRowItemBinding;
import edu.uncc.gradesapp.databinding.FragmentCourseReviewsBinding;
import edu.uncc.gradesapp.models.Course;
import edu.uncc.gradesapp.models.CourseReview;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CourseReviewsFragment extends Fragment {

    public static final String TAG = "debug";

    public CourseReviewsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    FragmentCourseReviewsBinding binding;
    ArrayList<CourseReview> mCourseReviews = new ArrayList<>();
    ArrayList<Course> mCourses = new ArrayList<>();
    CourseReviewsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCourseReviewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Course Reviews");

        // Setup RecyclerView and Adapter
        adapter = new CourseReviewsAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        // Fetch Courses from API
        getCourses();
    }

    private final OkHttpClient client = new OkHttpClient(); // HTTP Client

    private void getCourses(){

        // HTTP Request
        Request request = new Request.Builder()
                .url("https://www.theappsdr.com/api/cci-courses")
                .build();

        // Enqueue the request - Asynchronous
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { // Background Thread
                if(response.isSuccessful()){ // 200 OK
                    String body = response.body().string(); // JSON String of Courses from API Response Body
                    try {
                        mCourses.clear();
                        JSONObject json = new JSONObject(body); // JSON Object of Courses
                        JSONArray courses = json.getJSONArray("courses");  // JSON Array of Courses
                        for (int i = 0; i < courses.length(); i++) {
                            Course course = new Course(courses.getJSONObject(i));
                            mCourses.add(course);
                        }

                        // Update UI on Main Thread
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Insert courses into Firebase
                    InsertCoursesIntoFirebase();

                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Failed to get courses", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    // Insert Courses into Firebase
    private void InsertCoursesIntoFirebase() {

        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestore Instance

        db.collection("coursesCollection").get().addOnSuccessListener(queryDocumentSnapshots -> { // Fetch Courses from Firestore
            if(queryDocumentSnapshots.isEmpty()){ // If Courses Collection is Empty, create a HashMap for each Course and Insert into Firestore
                Log.d(TAG, "coursesCollection is Empty! Inserting Courses Fetched from API...");

                for (Course course : mCourses) {
                    HashMap<String, Object> dataToSave = new HashMap<>();
                    dataToSave.put("CourseInfo", course);
                    dataToSave.put("FavoriteByIDs", Arrays.asList(" - "));
                    dataToSave.put("ReviewCount", 0);

                    db.collection("coursesCollection").document(course.getNumber()).set(dataToSave) // Set Course Document
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Course Inserted: " + course.getNumber());
                            }).addOnFailureListener(e -> {
                                Log.d(TAG, "Failed to insert course: " + e.getMessage());
                            });
                }
            } else {
                Log.d(TAG, "coursesCollection is NOT Empty! Skipping Insertion...");
            }
        });
    }

    ListenerRegistration listenerRegistration;
    // RecyclerView Adapter
    class CourseReviewsAdapter extends RecyclerView.Adapter<CourseReviewsAdapter.CourseReviewsViewHolder> {

        @NonNull
        @Override
        public CourseReviewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CourseReviewRowItemBinding itemBinding = CourseReviewRowItemBinding.inflate(getLayoutInflater(), parent, false);
            return new CourseReviewsViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull CourseReviewsViewHolder holder, int position) {
            holder.setupUI(mCourses.get(position));
        }

        @Override
        public int getItemCount() {
            return mCourses.size();
        }

        // ViewHolder Class
        class CourseReviewsViewHolder extends RecyclerView.ViewHolder {

            CourseReviewRowItemBinding itemBinding;
            Course mCourse;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

            // Constructor
            public CourseReviewsViewHolder(CourseReviewRowItemBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;

                // Click Listener for each Course
                this.itemBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.gotoReviewCourse(mCourse);
                    }
                });
            }

            // Setup UI
            @SuppressLint("SetTextI18n")
            public void setupUI(Course course){
                this.mCourse = course;

                itemBinding.textViewCourseName.setText(course.getName());
                itemBinding.textViewCreditHours.setText(course.getHours() + " Credit Hours");
                itemBinding.textViewCourseNumber.setText(course.getNumber());

                // Initial check for Favorite Courses - Check if the Course is Favorited by the Current User
                // querySnapshot is an object that contains the results of the query
                listenerRegistration = db.collection("coursesCollection")
                        .whereEqualTo("CourseInfo.courseId", mCourse.getCourseId())
                        //.whereArrayContains("FavoriteByIDs", mUser.getUid())
                        .addSnapshotListener((querySnapshot, error) -> {
                            if (error != null) {
                                Log.w(TAG, "Error Checking Favorites!", error);
                                return;
                            }

                            // DocumentSnapshots are the actual documents. These are contained within the querySnapshot.getDocuments() method... which returns a list of DocumentSnapshots
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) { // Loop through each document
                                long reviewCount = (long) document.getData().get("ReviewCount"); // Get Review Count
                                ArrayList<String> favoriteByIDs = (ArrayList<String>) document.getData().get("FavoriteByIDs"); // Get FavoriteByIDs List
                                boolean isFavorite = favoriteByIDs.contains(mUser.getUid()); // Check if the Current User is in the FavoriteByIDs List
                                if (isFavorite) {
                                    itemBinding.imageViewHeart.setImageResource(R.drawable.ic_heart_full); // Set Heart Icon to Full
                                } else {
                                    itemBinding.imageViewHeart.setImageResource(R.drawable.ic_heart_empty); // Set Heart Icon to Empty
                                }
                                itemBinding.textViewCourseReviews.setText(reviewCount + " Reviews"); // Set Review Count Text
                            }
                        });

                /*
                CollectionReference coursesCollection = db.collection("coursesCollection");
                coursesCollection.document(course.getNumber()).get().addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        ArrayList<String> favoriteByIDs = (ArrayList<String>) documentSnapshot.get("FavoriteByIDs");
                        if(favoriteByIDs.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                            itemBinding.imageViewHeart.setImageResource(R.drawable.ic_heart_full);
                        } else {
                            itemBinding.imageViewHeart.setImageResource(R.drawable.ic_heart_empty);
                        }
                    }
                });
                 */

                itemBinding.imageViewHeart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        db.collection("coursesCollection")
                                .whereEqualTo("CourseInfo.courseId", mCourse.getCourseId())
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                                    DocumentReference docRef = document.getReference();
                                    ArrayList<String> favoriteByIDs = (ArrayList<String>) document.get("FavoriteByIDs");

                                    if(favoriteByIDs.contains(mUser.getUid())){
                                        // if the user is already in the list, remove them
                                        docRef.update("FavoriteByIDs", FieldValue.arrayRemove(mUser.getUid()))
                                                .addOnSuccessListener(aVoid -> {
                                                    itemBinding.imageViewHeart.setImageResource(R.drawable.ic_heart_empty);
                                                }).addOnFailureListener(e -> {
                                                    Log.d(TAG, "Failed to remove favorite: " + e.getMessage());
                                                });
                                        Log.d(TAG, "Removed from Favorites: " + mCourse.getNumber());
                                    } else {
                                        // if the user is not in the list, add them
                                        docRef.update("FavoriteByIDs", FieldValue.arrayUnion(mUser.getUid()))
                                                .addOnSuccessListener(aVoid -> {
                                                    itemBinding.imageViewHeart.setImageResource(R.drawable.ic_heart_full);
                                                }).addOnFailureListener(e -> {
                                                    Log.d(TAG, "Failed to add favorite: " + e.getMessage());
                                                });
                                        Log.d(TAG, "Added to Favorites: " + mCourse.getNumber());
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.d(TAG, "Failed to update favorite: " + e.getMessage());
                                });
                    }
                });
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.cancel_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_cancel){
            mListener.onSelectionCanceled();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    CourseReviewsListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (CourseReviewsListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " must implement CourseReviewsListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
    }

    public interface CourseReviewsListener{
        void onSelectionCanceled();
        void gotoReviewCourse(Course course);
    }
}