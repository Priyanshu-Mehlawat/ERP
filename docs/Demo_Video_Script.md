# 🎬 University ERP System - Demo Video Script (4-5 Minutes)

**Target Duration**: 4-5 minutes  
**Format**: Screen recording with narration  
**Date**: October 2025

---

## 🎯 Video Structure Overview

| Section | Duration | Content |
|---------|----------|---------|
| Introduction | 30 sec | Project overview |
| Admin Demo | 1:30 min | User & system management |
| Instructor Demo | 1:30 min | Grade management |
| Student Demo | 1:00 min | Course registration & grades |
| Security Features | 30 sec | Authentication & permissions |
| Closing | 10 sec | Summary |

---

## 📝 COMPLETE SCRIPT

### **[0:00 - 0:30] INTRODUCTION (30 seconds)**

**[SCREEN: Show login screen]**

**NARRATION:**
> "Hello! This is a demonstration of the University ERP Management System - a comprehensive desktop application built with Java Swing for managing university operations.
>
> The system features three user roles: Students, Instructors, and Administrators, each with role-specific dashboards and secure access control.
>
> Let's start by exploring the administrator features."

**ACTIONS:**
- Show the clean login screen with modern FlatLaf theme
- Briefly highlight the three login credentials visible on screen

---

### **[0:30 - 2:00] ADMIN FEATURES (1 minute 30 seconds)**

**[SCREEN: Login as admin1]**

**NARRATION:**
> "Logging in as an administrator with username 'admin1'..."

**ACTIONS:**
- Type: `admin1` / `password123`
- Click Login
- **[Wait for Admin Dashboard to load]**

---

**[SCREEN: Admin Dashboard - User Management]**

**NARRATION:**
> "The Admin Dashboard provides complete system management. First, let's look at User Management where we can create, edit, and manage users across all roles."

**ACTIONS:**
- Click "User Management" tab
- Show the user list table with existing users
- Click "Add User" button briefly (don't complete - just show the form)
- Highlight the Status column showing ACTIVE/LOCKED states

**NARRATION:**
> "Administrators can create new users, update their information, and manage account status including locking accounts for security."

---

**[SCREEN: Course Management]**

**NARRATION:**
> "Next is Course Management where we maintain the course catalog."

**ACTIONS:**
- Click "Course Management" tab
- Show the courses table (CSE101, CSE201, etc.)
- Click "Add Course" to show the form briefly
- Highlight key fields: Course Code, Title, Credits

**NARRATION:**
> "We can add new courses, update descriptions, and manage credit hours for all programs."

---

**[SCREEN: Section Management]**

**NARRATION:**
> "Section Management allows us to create class sections, assign instructors, set schedules, and manage capacity."

**ACTIONS:**
- Click "Section Management" tab
- Show sections table with instructor assignments
- Point out the capacity and enrolled columns
- Highlight the schedule times

**NARRATION:**
> "Notice how each section tracks capacity and current enrollment, with automated seat availability management."

---

**[SCREEN: Settings Panel]**

**NARRATION:**
> "The Settings panel controls system-wide configuration including the current semester, deadlines, and maintenance mode."

**ACTIONS:**
- Click "Settings" tab
- Show current semester and year
- Point to Add/Drop deadline
- Point to Withdrawal deadline
- Briefly hover over Maintenance Mode checkbox

**NARRATION:**
> "When maintenance mode is enabled, the system becomes read-only for all users, allowing safe system updates."

**ACTION:** Logout by clicking Logout button

---

### **[2:00 - 3:30] INSTRUCTOR FEATURES (1 minute 30 seconds)**

**[SCREEN: Back to Login]**

**NARRATION:**
> "Now let's see what instructors can do. Logging in as 'inst1'..."

**ACTIONS:**
- Login with `inst1` / `password123`
- **[Wait for Instructor Dashboard]**

---

**[SCREEN: Instructor Dashboard - My Sections]**

**NARRATION:**
> "The Instructor Dashboard shows all assigned teaching sections. I can see my sections for Fall 2025 here."

**ACTIONS:**
- Show "My Sections" tab with sections listed
- Click on one section to select it

---

**[SCREEN: Grade Entry Panel]**

**NARRATION:**
> "In the Grade Entry panel, I can enter scores for different assessment components like assignments, quizzes, midterms, and finals."

**ACTIONS:**
- Click "Grade Entry" tab
- Select a section from dropdown
- Show the enrolled students list
- Click on a student
- Show grade components (Assignment, Quiz, Midterm, Final)
- Briefly show weight configuration (don't modify)

**NARRATION:**
> "Each component has a weight, and the system automatically calculates final grades once all components total 100%."

**ACTIONS:**
- Click "Compute Final Grades" button to show the functionality
- Show the confirmation dialog (click Yes or No)

---

**[SCREEN: Class Statistics]**

**NARRATION:**
> "The Statistics panel provides class performance analytics including grade distribution."

**ACTIONS:**
- Click "Statistics" tab
- Show grade distribution chart or table
- Point out average, min, max scores

---

**[SCREEN: Reports & Export]**

**NARRATION:**
> "Instructors can also export grades to CSV for external analysis."

**ACTIONS:**
- Click "Reports" tab
- Click "Export Grades to CSV" button briefly (show file dialog, cancel it)

**ACTION:** Logout

---

### **[3:30 - 4:30] STUDENT FEATURES (1 minute)**

**[SCREEN: Back to Login]**

**NARRATION:**
> "Finally, let's see the student experience. Logging in as 'stu1'..."

**ACTIONS:**
- Login with `stu1` / `password123`
- **[Wait for Student Dashboard]**

---

**[SCREEN: Student Dashboard - Course Catalog]**

**NARRATION:**
> "Students can browse the course catalog and see available sections with real-time seat availability."

**ACTIONS:**
- Show "Course Catalog" tab
- Show courses listed with sections
- Point to "Available Seats" column
- Highlight a section with seats available

**NARRATION:**
> "The system tracks seat availability in real-time. Let's try registering for a course."

**ACTIONS:**
- Select an open section
- Click "Register" button
- **[Show success message or seat full message]**

---

**[SCREEN: My Timetable]**

**NARRATION:**
> "The Timetable shows the student's complete schedule with automatic conflict detection."

**ACTIONS:**
- Click "My Timetable" tab
- Show the visual schedule layout
- Point to enrolled sections with times

---

**[SCREEN: My Grades]**

**NARRATION:**
> "Students can view their grades for each enrolled section, broken down by component."

**ACTIONS:**
- Click "My Grades" tab
- Show grades table with sections
- Show components (Assignment, Quiz, etc.) and final grade

---

**[SCREEN: Transcript]**

**NARRATION:**
> "And finally, students can download their official transcript as a PDF."

**ACTIONS:**
- Click "Transcript" tab
- Click "Download Transcript" button
- Show PDF save dialog briefly (cancel it)

**ACTION:** Logout

---

### **[4:30 - 5:00] SECURITY & CLOSING (30 seconds)**

**[SCREEN: Back to Login]**

**NARRATION:**
> "The system includes robust security features. Let me demonstrate the account lockout mechanism."

**ACTIONS:**
- Try to login with `stu1` / `wrongpassword`
- Show error: "Invalid credentials. Attempt 1 of 5"
- Try again with wrong password
- Show error: "Invalid credentials. Attempt 2 of 5"

**NARRATION:**
> "After 5 failed login attempts, the account is automatically locked for security. All passwords are hashed using BCrypt, and the system enforces role-based access control to ensure users only see features they're authorized to use."

---

**[SCREEN: Return to login screen]**

**NARRATION:**
> "This University ERP System is production-ready with 97 comprehensive tests passing, complete documentation, and enterprise-grade security. The system successfully manages student enrollment, instructor grading, and administrative operations with a modern, intuitive interface.
>
> Thank you for watching!"

**ACTIONS:**
- Show login screen one final time
- Fade to black or end recording

---

## 🎥 RECORDING TIPS

### **Before Recording:**
1. ✅ Close unnecessary windows and notifications
2. ✅ Set screen resolution to 1920x1080 (or 1280x720)
3. ✅ Test audio levels
4. ✅ Prepare test data (ensure seed data is loaded)
5. ✅ Practice the script once without recording

### **During Recording:**
1. ✅ Speak clearly and at a moderate pace
2. ✅ Move mouse slowly and deliberately
3. ✅ Pause 2 seconds after each major action
4. ✅ Keep cursor away from important text
5. ✅ If you make a mistake, pause 5 seconds and restart that section

### **Recording Settings:**
- **Frame Rate**: 30 FPS
- **Quality**: High (but not MAX to keep file size reasonable)
- **Audio**: 48kHz, clear narration
- **Cursor**: Show cursor (default)

---

## 🎬 ALTERNATIVE: SHORTER VERSION (3-4 minutes)

If you need a shorter video, skip these sections:
- ❌ Course Management detail (just show it briefly)
- ❌ Class Statistics detail
- ❌ Transcript download (just mention it)
- ⚠️ Shorten security demo to just mention it

---

## 📊 SCREEN RECORDING TOOLS

### **For Linux (Ubuntu):**

```bash
# Option 1: Kazam (Simple, recommended)
sudo apt install kazam
kazam

# Option 2: SimpleScreenRecorder (More options)
sudo apt install simplescreenrecorder
simplescreenrecorder

# Option 3: OBS Studio (Professional)
sudo apt install obs-studio
obs
```

### **Recording Checklist:**
- [ ] Audio input selected (microphone)
- [ ] Screen area selected (full screen or window)
- [ ] Frame rate set to 30 FPS
- [ ] Output format: MP4 (H.264)
- [ ] Audio quality: Good
- [ ] Test recording 10 seconds before full demo

---

## 🎯 KEY MESSAGES TO EMPHASIZE

1. **Complete Solution**: All three roles implemented
2. **Security**: BCrypt, account lockout, role-based access
3. **Real-time**: Seat availability, conflict detection
4. **Modern UI**: Clean, intuitive, professional
5. **Production Ready**: 97 tests, comprehensive documentation

---

## 📝 POST-PRODUCTION (OPTIONAL)

If you want to edit after recording:

1. **Add Title Screen** (first 3 seconds)
   - "University ERP Management System"
   - "Java Swing Application"
   - Your name/team

2. **Add Section Labels** (optional)
   - Text overlay: "Admin Features", "Instructor Features", etc.

3. **Add Closing Screen** (last 3 seconds)
   - "Thank You"
   - "Built with: Java 17, MySQL, Maven"
   - Contact information (if needed)

### **Simple Video Editors for Linux:**
```bash
# OpenShot (Easy to use)
sudo apt install openshot-qt

# Kdenlive (More features)
sudo apt install kdenlive
```

---

## ✅ FINAL CHECKLIST

Before recording:
- [ ] Application running successfully
- [ ] Database has seed data
- [ ] All three users can login (admin1, inst1, stu1)
- [ ] Audio equipment working
- [ ] Screen recorder installed and tested
- [ ] Script practiced at least once
- [ ] Notifications disabled
- [ ] Clean desktop background

---

## 🎉 GOOD LUCK!

**Estimated Total Time:**
- Script practice: 15-20 minutes
- Recording attempts: 20-30 minutes (2-3 takes)
- Optional editing: 15-20 minutes
- **Total: 1-1.5 hours**

Your application is impressive - the demo will show it off beautifully! 🚀

---

**Created**: October 2025  
**For**: University ERP System Demo Video  
**Target Length**: 4-5 minutes  
**Status**: Ready to Record ✅
