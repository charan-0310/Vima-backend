<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Password Reset</title>
</head>
<body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
    <tr>
      <td align="center">
        <table width="500" cellpadding="0" cellspacing="0"
               style="background:#ffffff;border-radius:12px;padding:30px;
                      box-shadow:0 4px 20px rgba(0,0,0,0.05);">

          <!-- Logo -->
          <tr>
            <td align="center" style="padding-bottom:20px;">
              <img src="https://vimainsurance.com/assets/vima-logo-main.DiciI4hf.png"
                   alt="VIMA"
                   width="140"
                   style="display:block;">
            </td>
          </tr>

          <!-- Title -->
          <tr>
            <td align="center" style="font-size:20px;font-weight:bold;color:#111827;">
              Reset Your Password
            </td>
          </tr>

          <!-- Message -->
          <tr>
            <td style="padding:20px 0;color:#374151;font-size:14px;text-align:center;">
              Hello ${user.firstName!},<br/><br/>
              Click the button below to reset your password.
              This link will expire in ${linkExpiration} minutes.
            </td>
          </tr>

          <!-- Button -->
          <tr>
            <td align="center">
              <a href="${link}"
                 style="background:#2563eb;
                        color:#ffffff;
                        padding:12px 24px;
                        border-radius:8px;
                        text-decoration:none;
                        font-size:14px;
                        font-weight:bold;
                        display:inline-block;">
                Reset Password
              </a>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="padding-top:30px;color:#6b7280;font-size:12px;text-align:center;">
              If you did not request this password reset, please ignore this email.
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
