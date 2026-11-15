# CoNest

**Find your people. Feel at home.**

CoNest is a lifestyle-based flatmate discovery app. Instead of matching people on rent and location alone, it looks at how they actually live — sleep schedules, tidiness, guests, noise, chores — and helps them find someone they can genuinely share a home with.

Launching first in Hyderabad, Bengaluru, Pune and Kolkata.

## Features (MVP, in progress)

- Email and Google sign-in
- Onboarding: room intent (looking / has a room), profile photo, basics, neighbourhood, budget, move-in date, room details and photos, bio and interests
- A 10-question lifestyle questionnaire
- Weighted compatibility scoring across cleanliness, sleep, social habits, noise, work routine, chores, privacy and food, with deal-breakers for pets and smoking
- Returning users land on their saved profile

Coming next: discovery, likes and mutual matches, chat, and report/block.

## Tech

- Kotlin, Jetpack Compose, Material 3 (custom theme)
- MVVM with Hilt, Navigation Compose, Coroutines and Flow
- Firebase Authentication and Cloud Firestore
- Coil for images

Photos are stored in Firestore (compressed to ~1080px) so the project runs on Firebase's free Spark plan.

## Getting started

1. Open the project in Android Studio.
2. Create a Firebase project and add an Android app with package name `com.conest.app`.
3. Enable **Authentication** (Email/Password and Google) and add your debug SHA-1 under Project settings.
4. Create a **Firestore** database and publish the rules in [`firestore.rules`](firestore.rules).
5. Download `google-services.json` into `app/` (it is git-ignored).
6. Build and run.

Without `google-services.json` the app still builds and shows a setup notice.

## Project structure

```
app/src/main/java/com/conest/app/
├── data/          # models and Firebase-backed repositories
├── domain/        # lifestyle questions and compatibility calculation
├── di/            # Hilt modules
├── navigation/    # app routes and post-auth routing
└── ui/            # screens, view models, theme and shared components
```

## Tests

```
./gradlew testDebugUnitTest
```
