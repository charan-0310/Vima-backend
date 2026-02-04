# Employee Self-Service Enrollment - Functional Guide

> **For:** Product Managers, HR Teams, Business Stakeholders
> **Version:** 1.0
> **Last Updated:** February 4, 2026

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [User Personas](#2-user-personas)
3. [Complete Workflow](#3-complete-workflow)
4. [User Journeys](#4-user-journeys)
5. [Key Features](#5-key-features)
6. [Screens & Interactions](#6-screens--interactions)
7. [Business Rules](#7-business-rules)
8. [FAQ](#8-faq)

---

## 1. Feature Overview

### **What is Self-Service Enrollment?**

Self-Service Enrollment allows **employees** to complete their insurance enrollment **online**, without HR needing to collect and upload their details manually.

### **The Problem We're Solving**

**Before (CSV Upload):**
```
HR manually collects:
- Employee details
- Dependent information
- Nominee details
- Plan preferences

↓ Takes weeks to gather

HR creates CSV file with 500+ rows
HR uploads to VIMA
HR fixes errors, re-uploads
```

**After (Self-Service):**
```
HR uploads basic employee list (name, email, DOB)
System sends magic links to employees
Employees fill their own details (10 minutes)
HR reviews and approves
System creates endorsement automatically
```

### **Time Savings**

| Task | Before | After | Savings |
|------|--------|-------|---------|
| Data Collection | 2-3 weeks | 2-5 days | **80%** |
| Error Fixing | 3-5 days | < 1 day | **70%** |
| HR Effort | 40 hours | 8 hours | **80%** |
| **Total Time** | **4 weeks** | **1 week** | **75%** |

---

## 2. User Personas

### **👨‍💼 Persona 1: VIMA Admin (Raj)**
- **Role:** VIMA Internal Administrator
- **Responsibilities:** System configuration, company onboarding
- **Goal:** Set up enrollment for new companies efficiently
- **Pain Points:** Different companies have different rules (max dependents, coverage types)

### **👩‍💼 Persona 2: HR Admin (Priya)**
- **Role:** Company HR Manager
- **Responsibilities:** Employee benefits management
- **Goal:** Complete annual enrollment with minimum hassle
- **Pain Points:** 
  - Chasing employees for details
  - Manual data entry errors
  - Can't track who completed vs who's pending

### **👨‍💻 Persona 3: Employee (Amit)**
- **Role:** Software Engineer
- **Responsibilities:** Complete insurance enrollment
- **Goal:** Enroll family members quickly, understand coverage
- **Pain Points:**
  - Confusing forms
  - Doesn't know what documents needed
  - Forgets to submit on time

---

## 3. Complete Workflow

### **High-Level Flow**

```
┌────────────────────────────────────────────────────────────────────┐
│                         ANNUAL ENROLLMENT                          │
└────────────────────────────────────────────────────────────────────┘
                                 │
                                 ↓
┌────────────────────────────────────────────────────────────────────┐
│ STEP 1: HR SETUP (5 minutes)                                      │
├────────────────────────────────────────────────────────────────────┤
│ • Create enrollment window (dates: March 1-31, 2026)              │
│ • Upload employee list CSV (name, email, DOB, grade)              │
│ • System creates DRAFT ENDORSEMENT with all employees             │
│ • System sends magic link emails to all employees                 │
└────────────────────────────────────────────────────────────────────┘
                                 │
                                 ↓
┌────────────────────────────────────────────────────────────────────┐
│ STEP 2: EMPLOYEES ENROLL (10 minutes per employee)                │
├────────────────────────────────────────────────────────────────────┤
│ • Employee clicks magic link in email                              │
│ • Verifies personal details                                        │
│ • Adds family members (spouse, children, parents)                 │
│ • Selects coverage plans (GMC, GTL, GPA)                           │
│ • Nominates beneficiaries                                          │
│ • Reviews and submits                                              │
│                                                                     │
│ [System tracks: "4 out of 7 employees completed"]                 │
└────────────────────────────────────────────────────────────────────┘
                                 │
                                 ↓
┌────────────────────────────────────────────────────────────────────┐
│ STEP 3: HR REVIEWS (2 minutes per submission)                     │
├────────────────────────────────────────────────────────────────────┤
│ • HR sees pending approvals dashboard                              │
│ • Reviews each submission (details, dependents, plans)             │
│ • Approves or rejects with reason                                  │
│ • System updates endorsement member status                         │
└────────────────────────────────────────────────────────────────────┘
                                 │
                                 ↓
┌────────────────────────────────────────────────────────────────────┐
│ STEP 4: WINDOW CLOSES & FINALIZATION (1 minute)                   │
├────────────────────────────────────────────────────────────────────┤
│ • Enrollment window closes on March 31                             │
│ • HR sees summary: "4 completed, 3 incomplete"                     │
│ • HR finalizes endorsement (include/exclude incomplete)            │
│ • System creates final endorsement for insurer                     │
│ • Email notifications sent to all stakeholders                     │
└────────────────────────────────────────────────────────────────────┘
```

---

### **Detailed Process Flow**

```
VIMA ADMIN                  HR ADMIN                   EMPLOYEE                  SYSTEM
    │                          │                          │                         │
    │                          │                          │                         │
    ├─ Configure Company ──────>                          │                         │
    │  (max dependents,         │                          │                         │
    │   plan rules)             │                          │                         │
    │                          │                          │                         │
    │                          ├─ Create Window ──────────────────────────────────>│
    │                          │  (March 1-31, 2026)       │                         │
    │                          │                          │                         │
    │                          ├─ Upload Employee CSV ────────────────────────────>│
    │                          │  (500 employees)          │                         │
    │                          │                          │                         │
    │                          │                          │   <─── Create Draft ────┤
    │                          │                          │        Endorsement      │
    │                          │                          │        (500 members)    │
    │                          │                          │                         │
    │                          │                          │   <─── Send Magic Link ─┤
    │                          │                          │        Emails (500x)    │
    │                          │                          │                         │
    │                          │                          ├─ Click Email Link ─────>│
    │                          │                          │                         │
    │                          │                          │   <─── Validate Token ──┤
    │                          │                          │        Show Form        │
    │                          │                          │                         │
    │                          │                          ├─ Fill Details ─────────>│
    │                          │                          │  (dependents, plans)    │
    │                          │                          │                         │
    │                          │                          ├─ Submit ───────────────>│
    │                          │                          │                         │
    │                          │                          │   <─── Update Status ───┤
    │                          │                          │        = 'submitted'    │
    │                          │                          │                         │
    │                          │   <─── Notification ─────────────────────────────┤
    │                          │        (New submission)   │                         │
    │                          │                          │                         │
    │                          ├─ Review Submission ──────────────────────────────>│
    │                          │                          │                         │
    │                          ├─ Approve ───────────────────────────────────────>│
    │                          │                          │                         │
    │                          │                          │   <─── Update Status ───┤
    │                          │                          │        = 'approved'     │
    │                          │                          │                         │
    │                          │   <─── Send Email ───────────────────────────────┤
    │                          │        (Approved)         │   <─── Send Email ─────┤
    │                          │                          │        (Approved)       │
    │                          │                          │                         │
    ├──────────── [Window Closes - March 31] ─────────────────────────────────────┤
    │                          │                          │                         │
    │                          │   <─── Progress Summary ─────────────────────────┤
    │                          │        (450/500 done)     │                         │
    │                          │                          │                         │
    │                          ├─ Finalize Endorsement ───────────────────────────>│
    │                          │  (approve final)          │                         │
    │                          │                          │                         │
    │                          │   <─── Endorsement ──────────────────────────────┤
    │                          │        Ready for Insurer  │                         │
    │                          │                          │                         │
```

---

## 4. User Journeys

### **Journey 1: HR Admin - Setting Up Enrollment**

**Scenario:** Priya (HR Manager at TechCorp) needs to start annual enrollment for 500 employees.

**Steps:**

1. **Login to VIMA Admin Portal**
   - URL: `https://admin.vima.co`
   - Role: HR_ADMIN for TechCorp

2. **Create Enrollment Window**
   ```
   Screen: Enrollment > Create New Window
   
   Form Fields:
   ├─ Name: "TechCorp Annual Enrollment 2026"
   ├─ Start Date: March 1, 2026
   ├─ End Date: March 31, 2026
   ├─ Reminder Frequency: Every 3 days
   └─ Configuration:
      ├─ Max Dependents: 7
      ├─ Parent Coverage: Enabled
      ├─ Mandatory Plans: GMC, GPA, GTL
      └─ Top-up: Enabled
   
   [Save Window] → Window created with ID: ABC-123
   ```

3. **Upload Employee List**
   ```
   CSV Format (simple):
   ┌──────────────────────────────────────────────────────┐
   │ employee_id | name         | email              | dob │
   ├──────────────────────────────────────────────────────┤
   │ EMP001      | Amit Sharma  | amit@techcorp.com  | ... │
   │ EMP002      | Neha Gupta   | neha@techcorp.com  | ... │
   └──────────────────────────────────────────────────────┘
   
   Upload File: employees.csv (500 rows)
   
   System validates:
   ✓ All emails valid
   ✓ All employees exist in database
   ✓ No duplicates
   
   [Send Invitations] button clicked
   ```

4. **System Actions (Automatic)**
   ```
   ⏱️  Creating draft endorsement... ✓ (2 seconds)
   ⏱️  Generating 500 magic links... ✓ (5 seconds)
   ⏱️  Sending invitation emails... ✓ (3 minutes)
   
   Results:
   ✓ Sent: 498 emails
   ✗ Failed: 2 emails (invalid addresses)
   
   Endorsement ID: END-2026-TECHCORP-001
   Status: DRAFT
   ```

5. **Monitor Progress**
   ```
   Dashboard shows:
   
   ╔═════════════════════════════════════════╗
   ║  TechCorp Annual Enrollment 2026        ║
   ║  Status: ACTIVE                         ║
   ║                                         ║
   ║  Progress: ████████████░░░░░░░  60%    ║
   ║                                         ║
   ║  📊 Invited:    500                     ║
   ║  📖 Opened:     420                     ║
   ║  ✍️  Submitted: 300                     ║
   ║  ✅ Approved:   250                     ║
   ║  ⏳ Pending:    200                     ║
   ║                                         ║
   ║  Days Remaining: 15                     ║
   ╚═════════════════════════════════════════╝
   
   [View Incomplete Employees] [Send Reminders]
   ```

**Time Taken:** 10 minutes (vs 2-3 weeks manual collection)

---

### **Journey 2: Employee - Completing Enrollment**

**Scenario:** Amit (Employee) receives email and needs to enroll his family.

**Steps:**

1. **Receive Email**
   ```
   ┌──────────────────────────────────────────────────────┐
   │ From: VIMA Insurance <noreply@vima.co>               │
   │ Subject: Action Required: Complete Your Insurance    │
   │          Enrollment (Deadline: March 31)             │
   ├──────────────────────────────────────────────────────┤
   │                                                       │
   │ Hi Amit,                                             │
   │                                                       │
   │ Your company has opened the annual insurance         │
   │ enrollment window. Please complete your enrollment   │
   │ by March 31, 2026.                                   │
   │                                                       │
   │ [Complete Enrollment Now]                            │
   │                                                       │
   │ This link is valid for 30 days and can only be      │
   │ used by you.                                         │
   │                                                       │
   │ Questions? Contact hr@techcorp.com                   │
   └──────────────────────────────────────────────────────┘
   ```

2. **Click Magic Link**
   ```
   URL: https://app.vima.co/enrollment/ab12cd34ef56...
   
   System validates:
   ✓ Token valid
   ✓ Not expired
   ✓ Not already completed
   
   → Redirects to enrollment form
   ```

3. **Step 1: Verify Your Details**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  Step 1 of 5: Verify Your Details                ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  Name: Amit Sharma                    [Read-only]║
   ║  Email: amit@techcorp.com             [Read-only]║
   ║  Date of Birth: 15-Jan-1990           [Read-only]║
   ║  Employee ID: EMP001                  [Read-only]║
   ║  Grade: L3                            [Read-only]║
   ║                                                   ║
   ║  Phone: [+91-9876543210            ] [Editable]  ║
   ║                                                   ║
   ║  ⚠️  Please verify these details are correct.    ║
   ║      Contact HR if any information is wrong.     ║
   ║                                                   ║
   ║              [Continue →]                        ║
   ╚═══════════════════════════════════════════════════╝
   ```

4. **Step 2: Add Family Members**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  Step 2 of 5: Add Your Dependents                ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  Who would you like to cover?                     ║
   ║                                                   ║
   ║  ┌───────────────────────────────────────────┐   ║
   ║  │ 👤 Spouse                                 │   ║
   ║  │ Name: Priya Sharma                        │   ║
   ║  │ DOB: 20-Mar-1992                          │   ║
   ║  │ Premium: ₹3,500/year [Edit] [Remove]     │   ║
   ║  └───────────────────────────────────────────┘   ║
   ║                                                   ║
   ║  ┌───────────────────────────────────────────┐   ║
   ║  │ 👶 Child                                  │   ║
   ║  │ Name: Aarav Sharma                        │   ║
   ║  │ DOB: 10-May-2018                          │   ║
   ║  │ Premium: ₹2,500/year [Edit] [Remove]     │   ║
   ║  └───────────────────────────────────────────┘   ║
   ║                                                   ║
   ║  [+ Add Another Dependent]                       ║
   ║                                                   ║
   ║  Total Dependent Premium: ₹6,000/year            ║
   ║  (Your share: ₹0, Employer pays: ₹6,000)         ║
   ║                                                   ║
   ║  [← Back]              [Continue →]              ║
   ╚═══════════════════════════════════════════════════╝
   ```

5. **Step 3: Choose Coverage Plans**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  Step 3 of 5: Select Your Coverage               ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  ✓ Health Insurance (GMC)        [Mandatory]     ║
   ║    Coverage: ₹5,00,000                            ║
   ║    Premium: ₹5,000/year (Employer paid)          ║
   ║    Covers: You + Spouse + Child                   ║
   ║                                                   ║
   ║  ✓ Personal Accident (GPA)       [Mandatory]     ║
   ║    Coverage: ₹10,00,000                           ║
   ║    Premium: ₹2,000/year (Employer paid)          ║
   ║    Covers: You only                               ║
   ║                                                   ║
   ║  ✓ Life Insurance (GTL)          [Mandatory]     ║
   ║    Coverage: 3× Annual Salary (₹15,00,000)       ║
   ║    Premium: ₹3,000/year (Employer paid)          ║
   ║    Covers: You only                               ║
   ║                                                   ║
   ║  □ Top-up Health (Optional)      [Optional]      ║
   ║    Additional ₹5,00,000 coverage                  ║
   ║    Premium: ₹1,500/year (You pay)                ║
   ║                                                   ║
   ║  Total Premium: ₹16,000/year                      ║
   ║  Your contribution: ₹0                            ║
   ║  Employer contribution: ₹16,000                   ║
   ║                                                   ║
   ║  [← Back]              [Continue →]              ║
   ╚═══════════════════════════════════════════════════╝
   ```

6. **Step 4: Nominate Beneficiaries**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  Step 4 of 5: Nominate Beneficiaries             ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  For Life Insurance (GTL):                        ║
   ║                                                   ║
   ║  Nominee 1:                                       ║
   ║  Name: Priya Sharma (Spouse)                      ║
   ║  Percentage: [60%]                                ║
   ║                                                   ║
   ║  Nominee 2:                                       ║
   ║  Name: Aarav Sharma (Child)                       ║
   ║  Percentage: [40%]                                ║
   ║                                                   ║
   ║  Total: 100% ✓                                    ║
   ║                                                   ║
   ║  [+ Add Another Nominee]                          ║
   ║                                                   ║
   ║  [← Back]              [Continue →]              ║
   ╚═══════════════════════════════════════════════════╝
   ```

7. **Step 5: Review & Submit**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  Step 5 of 5: Review & Submit                    ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  📋 Your Details                                  ║
   ║     Amit Sharma | EMP001 | amit@techcorp.com     ║
   ║                                                   ║
   ║  👨‍👩‍👦 Your Dependents (2)                          ║
   ║     • Priya Sharma (Spouse, 32 years)            ║
   ║     • Aarav Sharma (Child, 6 years)              ║
   ║                                                   ║
   ║  🏥 Your Coverage                                 ║
   ║     • GMC: ₹5,00,000                              ║
   ║     • GPA: ₹10,00,000                             ║
   ║     • GTL: ₹15,00,000                             ║
   ║                                                   ║
   ║  💰 Premium Summary                               ║
   ║     Total: ₹16,000/year                           ║
   ║     Your cost: ₹0 (Employer paid)                 ║
   ║                                                   ║
   ║  📝 Declaration                                   ║
   ║  ☑ I declare that all information provided is    ║
   ║    true and accurate to the best of my knowledge.║
   ║                                                   ║
   ║  [← Back]              [Submit Enrollment]       ║
   ╚═══════════════════════════════════════════════════╝
   ```

8. **Success!**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  ✅ Enrollment Submitted Successfully!            ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  Reference Number: ENR-2026-TECHCORP-00123        ║
   ║                                                   ║
   ║  Your enrollment has been submitted for HR        ║
   ║  approval. You will receive a confirmation        ║
   ║  email once approved.                             ║
   ║                                                   ║
   ║  What happens next?                               ║
   ║  1. HR reviews your submission                    ║
   ║  2. You receive approval email                    ║
   ║  3. Coverage starts from April 1, 2026            ║
   ║                                                   ║
   ║  [Download Summary PDF] [Close]                   ║
   ╚═══════════════════════════════════════════════════╝
   ```

**Time Taken:** 10 minutes (vs multiple days of back-and-forth)

---

### **Journey 3: HR Admin - Reviewing & Approving**

**Scenario:** Priya needs to review and approve employee submissions.

**Steps:**

1. **View Pending Approvals**
   ```
   ╔═══════════════════════════════════════════════════════════════╗
   ║  Pending Approvals                     [Filters ▼] [Export]  ║
   ╠═══════════════════════════════════════════════════════════════╣
   ║                                                               ║
   ║  ┌───────────────────────────────────────────────────────┐   ║
   ║  │ 👤 Amit Sharma (EMP001)                               │   ║
   ║  │    Submitted: Jan 15, 10:30 AM                        │   ║
   ║  │    Dependents: 2 (Spouse, 1 Child)                    │   ║
   ║  │    Premium: ₹16,000/year                              │   ║
   ║  │    [View Details] [Approve] [Reject]                  │   ║
   ║  └───────────────────────────────────────────────────────┘   ║
   ║                                                               ║
   ║  ┌───────────────────────────────────────────────────────┐   ║
   ║  │ 👤 Neha Gupta (EMP002)                                │   ║
   ║  │    Submitted: Jan 15, 2:45 PM                         │   ║
   ║  │    Dependents: 4 (Spouse, 2 Children, 1 Parent)      │   ║
   ║  │    Premium: ₹25,000/year                              │   ║
   ║  │    [View Details] [Approve] [Reject]                  │   ║
   ║  └───────────────────────────────────────────────────────┘   ║
   ║                                                               ║
   ║  Showing 2 of 150 pending submissions                        ║
   ║                                                               ║
   ║  [Select All] [Bulk Approve]                                 ║
   ╚═══════════════════════════════════════════════════════════════╝
   ```

2. **Review Submission Details**
   ```
   ╔═══════════════════════════════════════════════════════════════╗
   ║  Submission Details: Amit Sharma (ENR-2026-TECHCORP-00123)   ║
   ╠═══════════════════════════════════════════════════════════════╣
   ║                                                               ║
   ║  Employee Information                                         ║
   ║  ├─ Name: Amit Sharma                                         ║
   ║  ├─ Email: amit@techcorp.com                                  ║
   ║  ├─ Phone: +91-9876543210                                     ║
   ║  ├─ Grade: L3                                                 ║
   ║  └─ Date of Joining: 01-Apr-2019                              ║
   ║                                                               ║
   ║  Dependents                                                   ║
   ║  ├─ 1. Priya Sharma                                           ║
   ║  │     Relationship: Spouse                                   ║
   ║  │     DOB: 20-Mar-1992 (Age: 32)                             ║
   ║  │     Premium: ₹3,500/year                                   ║
   ║  │                                                             ║
   ║  └─ 2. Aarav Sharma                                           ║
   ║        Relationship: Child                                    ║
   ║        DOB: 10-May-2018 (Age: 6)                              ║
   ║        Premium: ₹2,500/year                                   ║
   ║                                                               ║
   ║  Coverage Plans                                               ║
   ║  ├─ GMC: ₹5,00,000 (₹5,000/year)                              ║
   ║  ├─ GPA: ₹10,00,000 (₹2,000/year)                             ║
   ║  └─ GTL: ₹15,00,000 (₹3,000/year)                             ║
   ║                                                               ║
   ║  Nominees (GTL)                                               ║
   ║  ├─ Priya Sharma (60%)                                        ║
   ║  └─ Aarav Sharma (40%)                                        ║
   ║                                                               ║
   ║  Total Premium: ₹16,000/year                                  ║
   ║                                                               ║
   ║  ✅ All validation checks passed                              ║
   ║                                                               ║
   ║  [Approve] [Reject with Reason]                               ║
   ╚═══════════════════════════════════════════════════════════════╝
   ```

3. **Approve or Reject**
   ```
   Option A: Approve
   ────────────────────
   Click [Approve]
   
   → System updates status to 'approved'
   → Sends email to Amit: "Your enrollment is approved"
   → Updates endorsement member status
   
   
   Option B: Reject
   ────────────────────
   Click [Reject with Reason]
   
   ╔═══════════════════════════════════════════════════╗
   ║  Reject Submission                                ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  Reason for rejection:                            ║
   ║  ┌─────────────────────────────────────────────┐ ║
   ║  │ Child's date of birth seems incorrect.      │ ║
   ║  │ Please verify and resubmit.                 │ ║
   ║  └─────────────────────────────────────────────┘ ║
   ║                                                   ║
   ║  [Cancel] [Reject & Notify Employee]             ║
   ╚═══════════════════════════════════════════════════╝
   
   → System updates status to 'rejected'
   → Sends email to Amit with rejection reason
   → Reopens enrollment link for Amit to resubmit
   ```

4. **Track Overall Progress**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  Enrollment Progress Dashboard                    ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  Window: TechCorp Annual 2026                     ║
   ║  Period: March 1-31, 2026                         ║
   ║  Days Remaining: 15                               ║
   ║                                                   ║
   ║  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ║
   ║                                                   ║
   ║  📊 Overall Progress                              ║
   ║                                                   ║
   ║       Completed: 300/500 (60%)                    ║
   ║       [████████████░░░░░░░░░░]                    ║
   ║                                                   ║
   ║  Status Breakdown:                                ║
   ║  ✅ Approved:   250 (50%)                         ║
   ║  ⏳ Pending:    150 (30%)                         ║
   ║  ❌ Rejected:    20 (4%)                          ║
   ║  📧 Not Opened: 80 (16%)                          ║
   ║                                                   ║
   ║  [View Incomplete Employees]                      ║
   ║  [Send Reminder to Pending]                       ║
   ║  [Bulk Approve All Pending]                       ║
   ║                                                   ║
   ╚═══════════════════════════════════════════════════╝
   ```

**Time Taken:** 2 minutes per submission (vs hours of data entry)

---

### **Journey 4: Finalization After Window Closes**

**Scenario:** Enrollment window closed on March 31. Priya needs to finalize the endorsement.

**Steps:**

1. **View Finalization Summary**
   ```
   ╔═══════════════════════════════════════════════════════════════╗
   ║  Enrollment Window Closed                                     ║
   ║  TechCorp Annual 2026 (March 1-31, 2026)                      ║
   ╠═══════════════════════════════════════════════════════════════╣
   ║                                                               ║
   ║  Final Results:                                               ║
   ║                                                               ║
   ║  Total Employees: 500                                         ║
   ║  ✅ Completed & Approved: 450 (90%)                           ║
   ║  ❌ Rejected/Incomplete: 50 (10%)                             ║
   ║                                                               ║
   ║  Endorsement Status: DRAFT                                    ║
   ║  Endorsement ID: END-2026-TECHCORP-001                        ║
   ║                                                               ║
   ║  What would you like to do?                                   ║
   ║                                                               ║
   ║  Option 1: Finalize with completed members only               ║
   ║  └─ 450 members will be included                              ║
   ║  └─ 50 incomplete members will be excluded                    ║
   ║                                                               ║
   ║  Option 2: Include all employees (even incomplete)            ║
   ║  └─ All 500 members will be included                          ║
   ║  └─ Incomplete members get basic coverage                     ║
   ║                                                               ║
   ║  [Finalize with 450 Members] [Include All 500]                ║
   ║                                                               ║
   ║  [View Incomplete Employees List]                             ║
   ╚═══════════════════════════════════════════════════════════════╝
   ```

2. **Finalize Endorsement**
   ```
   Click: [Finalize with 450 Members]
   
   ╔═══════════════════════════════════════════════════╗
   ║  Confirm Finalization                             ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  You are about to finalize the endorsement:       ║
   ║                                                   ║
   ║  • 450 approved members will be included          ║
   ║  • 50 incomplete members will be excluded         ║
   ║  • Endorsement status will change to APPROVED     ║
   ║  • This action cannot be undone                   ║
   ║                                                   ║
   ║  Total Annual Premium: ₹72,00,000                 ║
   ║  (Employer share: ₹70,00,000)                     ║
   ║  (Employee share: ₹2,00,000)                      ║
   ║                                                   ║
   ║  [Cancel] [Confirm & Finalize]                    ║
   ╚═══════════════════════════════════════════════════╝
   ```

3. **Success!**
   ```
   ╔═══════════════════════════════════════════════════╗
   ║  ✅ Endorsement Finalized Successfully!           ║
   ╠═══════════════════════════════════════════════════╣
   ║                                                   ║
   ║  Endorsement ID: END-2026-TECHCORP-001            ║
   ║  Status: APPROVED                                 ║
   ║  Final Member Count: 450                          ║
   ║                                                   ║
   ║  What's next?                                     ║
   ║  1. Endorsement sent to insurer for processing    ║
   ║  2. Policy certificates will be generated         ║
   ║  3. Coverage effective from April 1, 2026         ║
   ║                                                   ║
   ║  [Download Endorsement Report]                    ║
   ║  [View in Endorsements]                           ║
   ║  [Send Notifications to Employees]                ║
   ╚═══════════════════════════════════════════════════╝
   ```

---

## 5. Key Features

### **5.1 Magic Link Authentication**

**What is it?**
- Special secure link sent to employee email
- No passwords needed
- Link valid for 30 days
- Can only be used once

**Why it's better:**
- No login/password hassle
- More secure than shared passwords
- Easy for employees (just click link)
- Prevents unauthorized access

**Example:**
```
Magic Link: https://app.vima.co/enrollment/a1b2c3d4e5f6...

Token Structure:
├─ Random 256-bit token
├─ Hashed and stored in database
├─ Linked to specific employee
└─ Expires after 30 days or completion
```

---

### **5.2 Real-Time Progress Tracking**

**What is it?**
- Dashboard showing enrollment completion status
- See exactly who completed and who's pending
- Track progress daily

**Benefits:**
- Know who to follow up with
- No surprises at deadline
- Data-driven decision making

**Metrics Tracked:**
```
┌─────────────────────────────────────────┐
│ Total Invited:       500               │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│ Opened Link:         420 (84%)         │
│ Started Enrollment:  350 (70%)         │
│ Submitted:           300 (60%)         │
│ Approved:            250 (50%)         │
│                                        │
│ Avg. Time to Complete: 12 minutes     │
│ Avg. Approval Time: 1.5 minutes       │
└─────────────────────────────────────────┘
```

---

### **5.3 Draft Endorsement from Day 1**

**What is it?**
- When HR sends invitations, system creates endorsement immediately
- Status = "DRAFT"
- Tracks all employees from start

**Traditional Approach:**
```
Day 1:  Send invitations
Day 30: Collect submissions
Day 31: Create endorsement
```

**New Approach:**
```
Day 1:  Send invitations + Create draft endorsement
Day 1-30: Track progress in real-time
Day 31: Finalize endorsement (just update status)
```

**Benefits:**
- Single source of truth from day 1
- Easy tracking
- No data migration at end
- Cleaner workflow

---

### **5.4 Automatic Reminders**

**What is it?**
- System automatically sends reminder emails
- Configurable frequency (every 3/5/7 days)
- Final reminder 1 day before deadline

**Reminder Schedule:**
```
Day 1:   Initial invitation sent
Day 4:   First reminder (if not opened)
Day 7:   Second reminder (if not submitted)
Day 10:  Third reminder
Day 29:  Final reminder (deadline tomorrow!)
```

**Reduces HR workload by 80%** (no manual follow-ups needed)

---

### **5.5 Premium Calculator**

**What is it?**
- Real-time premium calculation as employee adds dependents
- Shows employer vs employee contribution
- Instant feedback

**Example:**
```
Base Coverage (Employee only):
├─ GMC: ₹5,000/year
├─ GPA: ₹2,000/year
└─ GTL: ₹3,000/year
    Total: ₹10,000/year (Employer paid)

Add Spouse:
└─ Additional: ₹3,500/year (Employer paid)
    New Total: ₹13,500/year

Add Child:
└─ Additional: ₹2,500/year (Employer paid)
    New Total: ₹16,000/year

Add Optional Top-up:
└─ Additional: ₹1,500/year (YOU pay)
    New Total: ₹17,500/year
    ├─ Employer pays: ₹16,000
    └─ You pay: ₹1,500
```

---

### **5.6 Validation & Error Prevention**

**What is it?**
- System validates data before submission
- Prevents common errors
- Clear error messages

**Validations:**
```
✓ Age Constraints:
  - Spouse: Must be 18+ years
  - Children: Must be < 25 years
  - Parents: Must be 50+ years

✓ Relationship Rules:
  - Max 1 spouse
  - Max 4 children
  - Max 4 parents (2 parents + 2 in-laws)

✓ Nominee Percentages:
  - Must sum to exactly 100%
  - Cannot be 0% or negative

✓ Document Requirements:
  - Dependent proof required (if configured)
  - Supported formats: PDF, JPG, PNG

✓ Data Completeness:
  - All mandatory fields filled
  - Valid email and phone formats
```

**Example Error Messages:**
```
❌ Invalid Date of Birth
   Child's age cannot exceed 25 years.
   Please verify the date of birth.

✓ Corrected and submitted successfully!
```

---

## 6. Screens & Interactions

### **Admin Portal Screens**

1. **Enrollment Windows List**
   - View all enrollment windows (past, active, upcoming)
   - Filter by company, status, dates
   - Create new window button

2. **Window Creation Form**
   - Name, dates, company selection
   - Configuration settings
   - Validation and preview

3. **Invitation Management**
   - Upload employee CSV
   - Review employees before sending
   - Bulk send invitations
   - Track send status

4. **Progress Dashboard**
   - Real-time metrics
   - Charts and graphs
   - Drill-down to individual employees

5. **Approval Queue**
   - List of pending submissions
   - Quick approve/reject actions
   - Bulk operations
   - Detailed submission view

6. **Endorsement Finalization**
   - Summary of completed/incomplete
   - Finalization options
   - Confirmation screen

---

### **Employee Portal Screens**

1. **Email Invitation**
   - Clean, professional design
   - Clear call-to-action button
   - Deadline and company info

2. **Step 1: Verification**
   - Pre-filled personal details
   - Editable contact info
   - Instructions and help text

3. **Step 2: Dependents**
   - Add/edit/remove dependents
   - Real-time premium calculation
   - Document upload (optional)

4. **Step 3: Plans**
   - Coverage options with descriptions
   - Mandatory vs optional indicators
   - Premium breakdown

5. **Step 4: Nominees**
   - Nominee form (name, relationship, %)
   - Percentage validation
   - Link to dependents

6. **Step 5: Review**
   - Complete summary
   - Declaration checkbox
   - Submit button

7. **Confirmation**
   - Reference number
   - Next steps
   - Download summary PDF

---

## 7. Business Rules

### **7.1 Enrollment Window Rules**

```
✓ Window Duration:
  - Minimum: 7 days
  - Maximum: 60 days
  - Recommended: 30 days

✓ Company Configuration:
  - Max dependents: 7 (configurable)
  - Parent coverage: Yes/No
  - Top-up: Yes/No
  - Mandatory plans: GMC, GPA, GTL

✓ Status Lifecycle:
  scheduled → active → closed
  
  - scheduled: Before start date
  - active: Between start and end dates
  - closed: After end date or manual close
```

---

### **7.2 Invitation Rules**

```
✓ One invitation per employee per window
✓ Token valid for window duration (min 7 days)
✓ Token expires when:
  - Window closes
  - Employee completes enrollment
  - 30 days elapsed (whichever comes first)

✓ Reminders sent when:
  - Invitation not opened (after 3 days)
  - Started but not submitted (after 3 days)
  - 1 day before deadline
```

---

### **7.3 Dependent Rules**

```
✓ Spouse:
  - Max: 1
  - Age: 18+ years
  - Proof: Marriage certificate (optional)

✓ Children:
  - Max: 4
  - Age: 0-24 years
  - Proof: Birth certificate (optional)

✓ Parents:
  - Max: 4 (2 parents + 2 parents-in-law)
  - Age: 50+ years
  - Proof: Age proof (optional)

✓ Premium Sharing:
  - Employer pays: X% (configurable)
  - Employee pays: (100-X)%
```

---

### **7.4 Approval Rules**

```
✓ HR can:
  - Approve individual submissions
  - Reject with reason
  - Bulk approve (max 100 at a time)

✓ Auto-approval (future):
  - If no dependents changed
  - If premium below threshold
  - If validation passed

✓ Rejected submissions:
  - Employee receives notification
  - Link reopened for resubmission
  - Original data preserved
```

---

### **7.5 Finalization Rules**

```
✓ Window must be closed
✓ Endorsement must be in DRAFT status
✓ Options:
  - Include only approved members
  - Include all (even incomplete)

✓ After finalization:
  - Status changes to APPROVED
  - Cannot be modified
  - Sent to insurer
```

---

## 8. FAQ

### **For Employees**

**Q: How long does the enrollment process take?**
A: Typically 10-15 minutes if you have all dependent information ready.

**Q: What if I make a mistake?**
A: You can edit your submission before final submission. After submission, contact HR to reject and allow you to resubmit.

**Q: Can I save and continue later?**
A: Yes! The system auto-saves your progress. You can close the browser and come back anytime using the same link.

**Q: What if the link expires?**
A: Contact your HR. They can extend your deadline and send a new link.

**Q: Do I need to upload documents?**
A: It depends on your company's policy. The system will show if documents are required.

**Q: Can I add dependents later?**
A: During the enrollment window, yes. After the window closes, you'll need to wait for a mid-term enrollment or next annual enrollment.

---

### **For HR Admins**

**Q: Can I edit employee details after sending invitations?**
A: Yes, but it's better to update the employee record in the main system first, then resend the invitation.

**Q: What if an employee doesn't complete enrollment?**
A: You can send reminders, extend their deadline, or include them with basic coverage during finalization.

**Q: Can I close the window early?**
A: Yes, manually close the window if all employees have completed.

**Q: What happens to rejected submissions?**
A: The employee receives an email with rejection reason and can resubmit before the deadline.

**Q: Can I export the data?**
A: Yes, you can export enrollment data to Excel at any time.

**Q: How do I handle mid-year joiners?**
A: Create a separate enrollment window specifically for new joiners.

---

### **For VIMA Admins**

**Q: Can companies have different rules?**
A: Yes, each company can have custom configuration (max dependents, coverage options, etc.).

**Q: How is the endorsement linked to the window?**
A: Each enrollment window creates one endorsement. The endorsement_id is stored in the window record.

**Q: What if enrollment fails for some employees?**
A: Failed employees are tracked separately. HR can review and retry or handle manually.

**Q: Can we integrate with company HRMS?**
A: Future enhancement. Currently, HR uploads CSV from their HRMS.

---

## Glossary

| Term | Definition |
|------|------------|
| **Enrollment Window** | Time period during which employees can complete enrollment (e.g., March 1-31) |
| **Magic Link** | Secure, one-time-use URL sent to employee email for enrollment access |
| **Draft Endorsement** | Endorsement created at the start with status='draft', finalized at the end |
| **Deal Endorsement** | Link between an employee/dependent and an endorsement in the database |
| **Submission** | Complete enrollment filled by an employee, pending HR approval |
| **Dependent** | Family member covered under employee's insurance (spouse, child, parent) |
| **Nominee** | Beneficiary who receives insurance payout in case of employee's death |
| **GMC** | Group Mediclaim (Health Insurance) |
| **GPA** | Group Personal Accident |
| **GTL** | Group Term Life Insurance |
| **Finalization** | Process of converting draft endorsement to approved status after window closes |

---

## Support & Contact

**For Technical Issues:**
- Email: support@vima.co
- Phone: 1800-123-4567
- Live Chat: Available 9 AM - 6 PM IST

**For HR Queries:**
- Email: hr-support@vima.co
- Documentation: https://docs.vima.co

**For Employees:**
- Contact your company HR team
- Help Center: https://help.vima.co

---

*Document Version: 1.0*
*Created: February 4, 2026*
*For: TechCorp & VIMA Insurance Platform*
