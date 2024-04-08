package edu.uncc.gradesapp.fragments;

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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import edu.uncc.gradesapp.R;
import edu.uncc.gradesapp.databinding.FragmentReviewCourseBinding;
import edu.uncc.gradesapp.databinding.ReviewRowItemBinding;
import edu.uncc.gradesapp.models.Course;
import edu.uncc.gradesapp.models.CourseReview;
import edu.uncc.gradesapp.models.Review;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReviewCourseFragment extends Fragment {

    public static final String TAG = "debug";

    private static final String ARG_PARAM_COURSE = "ARG_PARAM_COURSE";
    private Course mCourse;
    ArrayList<Review> mReviews = new ArrayList<>();
    ReviewsAdapter adapter;

    public ReviewCourseFragment() {
        // Required empty public constructor
    }

    public static ReviewCourseFragment newInstance(Course course) {
        ReviewCourseFragment fragment = new ReviewCourseFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM_COURSE, course);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCourse = (Course) getArguments().getSerializable(ARG_PARAM_COURSE);
        }
        setHasOptionsMenu(true);
    }

    FragmentReviewCourseBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReviewCourseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Review Course");

        binding.textViewCourseName.setText(mCourse.getName());
        binding.textViewCourseNumber.setText(mCourse.getNumber());
        binding.textViewCreditHours.setText(String.valueOf(mCourse.getHours()) + " Credit Hours");

        adapter = new ReviewsAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reviewText = binding.editTextReview.getText().toString();
                if (reviewText.isEmpty()) {
                    Toast.makeText(getActivity(), "Review cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Review(String reviewText, String createdBy, String createdByUid, com.google.firebase.Timestamp createdOn)
                    Review review = new Review(reviewText, mUser.getDisplayName(), mUser.getUid(), new Timestamp(new Date()));

                    // value: [DocumentSnapshot{key=coursesCollection/ITSC 1212, metadata=SnapshotMetadata{hasPendingWrites=false, isFromCache=false}, doc=Document{key=coursesCollection/ITSC 1212, version=SnapshotVersion(seconds=1711872466, nanos=743468000), readTime=SnapshotVersion(seconds=1711872466, nanos=743468000), type=FOUND_DOCUMENT, documentState=SYNCED, value=ObjectValue{internalValue={CourseInfo:{courseId:e3a6ef8a-b1a8-4bc2-8f86-fb7f5c276c0b,hours:4.0,name:Introduction to Computer Science I,number:ITSC 1212},FavoriteByIDs:[ - ,7XFGiAiLHAOZxIGlbfwV1bjJ8zy2],ReviewCount:0}}}}]
                    // Value returns a QuerySnapshot object that contains the documents that match the query. In this case, since the query is based on the course number, there should be only one document in the QuerySnapshot object. Hence, we can get the first document using getDocuments().get(0) which is the only document in the QuerySnapshot object.
                    //Log.d(TAG, "value: " + value.getDocuments().get(0));

                    // Create a new SubCollection for reviews under the course document in the coursesCollection
                    db.collection("coursesCollection").whereEqualTo("CourseInfo.courseId", mCourse.getCourseId()).get()
                            .addOnSuccessListener(queryDocumentSnapshots -> { // queryDocumentSnapshots is the same as value in the above comment (in .snapshotListener)
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0); // get the first document in the QuerySnapshot object (since there should be only one document in the QuerySnapshot object)
                                    DocumentReference courseDocRef = documentSnapshot.getReference(); // get the reference to the course document

                                    // Create a new document for the reviews sub-collection under the course document
                                    courseDocRef.collection("ReviewsSubCollection").document().set(review)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getActivity(), "Review Submitted Successfully!", Toast.LENGTH_SHORT).show();
                                                Log.d(TAG, "Review Submitted Successfully!");

                                                // Update the review count in the course document
                                                long currentReviewCount = documentSnapshot.getLong("ReviewCount");
                                                long updatedReviewCount = (currentReviewCount == 0) ? 1 : currentReviewCount + 1; // if the currentReviewCount is 0, then set the updatedReviewCount to 1, else increment the currentReviewCount by 1
                                                courseDocRef.update("ReviewCount", updatedReviewCount)
                                                        .addOnSuccessListener(aVoid1 -> {
                                                            Log.d(TAG, "Review Count Updated Successfully!");
                                                            mListener.onSelectionCanceled(); // When this is called, the review is inserted, but app crashes
                                                        }).addOnFailureListener(e -> {
                                                            Toast.makeText(getActivity(), "Failed to Submit Review!", Toast.LENGTH_SHORT).show();
                                                            Log.d(TAG, "Failed to Submit Review!" + e.getMessage());
                                                        });
                                            }).addOnFailureListener(e -> {
                                                Toast.makeText(getActivity(), "Failed to Submit Review!", Toast.LENGTH_SHORT).show();
                                                Log.d(TAG, "Failed to Submit Review!" + e.getMessage());
                                            });
                                }
                            }).addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), "Failed to Submit Review!", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        // Get reviews from Firestore
        getReviewsFromFirestore();
    }

    private void getReviewsFromFirestore() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("coursesCollection").whereEqualTo("CourseInfo.courseId", mCourse.getCourseId()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        DocumentReference courseDocRef = documentSnapshot.getReference();

                        courseDocRef.collection("ReviewsSubCollection").get()
                                .addOnSuccessListener(queryDocumentSnapshots1 -> {
                                    mReviews.clear();

                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots1) {
                                        Review review = document.toObject(Review.class);
                                        mReviews.add(review);
                                        //Log.d(TAG, "getReviewsFromFirestore: " + document.getData());
                                    }

                                    adapter.notifyDataSetChanged();
                                    Log.d(TAG, "Total Reviews for This Course: " + mReviews.size());

                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to get reviews from SubCollection: " + e.getMessage());
                                });
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get reviews from Firestore: " + e.getMessage());
                });
    }


    class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
        @NonNull
        @Override
        public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ReviewRowItemBinding itemBinding = ReviewRowItemBinding.inflate(getLayoutInflater(), parent, false);
            return new ReviewViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
            Review review = mReviews.get(position);
            holder.setupUI(review);
        }

        @Override
        public int getItemCount() {
            return mReviews.size();
        }

        class ReviewViewHolder extends RecyclerView.ViewHolder {
            ReviewRowItemBinding itemBinding;
            Review mReview;

            public ReviewViewHolder(ReviewRowItemBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }


            private void setupUI(Review review) {
                this.mReview = review;

                itemBinding.textViewUserName.setText(review.getCreatedBy());
                itemBinding.textViewReview.setText(review.getReviewText());

                Date date = review.getCreatedOn().toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
                itemBinding.textViewCreatedAt.setText(sdf.format(date));

                itemBinding.imageViewDelete.setVisibility(View.INVISIBLE);

                // If the review is created by the current user, then show the delete icon
                if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(review.getCreatedByUid())) {
                    itemBinding.imageViewDelete.setVisibility(View.VISIBLE);

                    itemBinding.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            db.collection("coursesCollection").whereEqualTo("CourseInfo.courseId", mCourse.getCourseId()).get() // get the course document
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        if (!queryDocumentSnapshots.isEmpty()) {
                                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0); // get the first document in the QuerySnapshot object (since there should be only one document in the QuerySnapshot object) due to the whereEqualTo clause
                                            DocumentReference courseDocRef = documentSnapshot.getReference(); // get the reference to the course document

                                            // Delete the review from the SubCollection based on the createdByUid and reviewText
                                            courseDocRef.collection("ReviewsSubCollection")
                                                    .whereEqualTo("createdByUid", review.getCreatedByUid())
                                                    .whereEqualTo("reviewText", review.getReviewText())
                                                    .get()
                                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) { // queryDocumentSnapshots is the same as value in the above comment (in .snapshotListener)
                                                            // Delete the review from the SubCollection
                                                            DocumentSnapshot reviewDocument = queryDocumentSnapshots.getDocuments().get(0); // get the first document in the QuerySnapshot object (since there should be only one document in the QuerySnapshot object) due to the whereEqualTo clause
                                                            reviewDocument.getReference().delete() // delete the review document
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            Log.d(TAG, "Review Deleted Successfully!");
                                                                            getReviewsFromFirestore();
                                                                        }
                                                                    }).addOnFailureListener(e -> {
                                                                        Log.e(TAG, "Failed to Delete Review from SubCollection: " + e.getMessage());
                                                                    });

                                                            // Update the review count in the course document
                                                            long currentReviewCount = documentSnapshot.getLong("ReviewCount");
                                                            long updatedReviewCount = (currentReviewCount == 0) ? 0 : currentReviewCount - 1; // if the currentReviewCount is 0, then set the updatedReviewCount to 0, else decrement the currentReviewCount by 1
                                                            courseDocRef.update("ReviewCount", updatedReviewCount)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void aVoid) {
                                                                            Log.d(TAG, "Review Count Updated Successfully!");
                                                                        }
                                                                    }).addOnFailureListener(e -> {
                                                                        Log.e(TAG, "Failed to Update Review Count in Course Document: " + e.getMessage());
                                                                    });
                                                        }
                                                    });
                                        }
                                        //mListener.onSelectionCanceled();
                                    }).addOnFailureListener(e -> {
                                        Toast.makeText(getActivity(), "Failed to Delete Review from Firestore!", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
                }

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
        if (item.getItemId() == R.id.action_cancel) {
            mListener.onSelectionCanceled();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ReviewCourseListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (ReviewCourseListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ReviewCourseListener");
        }
    }

    public interface ReviewCourseListener {
        void onSelectionCanceled();
    }
}