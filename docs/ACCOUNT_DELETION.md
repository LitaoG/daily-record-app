# 账号与数据删除指引 / Account & Data Deletion Guide

状态：`current reference — Google Play 开发者政策合规网页资源规范`  
最后复核：2026-11-07  
依据：[Google Play 开发者政策：了解应用账号删除要求](https://support.google.com/googleplay/android-developer/answer/13327111)、[ADR-022](DECISIONS.md)、[PLAY_STORE_READINESS.md](product/PLAY_STORE_READINESS.md)

---

## 中文说明 (Chinese)

“私密日历”（Daily Record）严格遵守 Google Play 开发者政策关于“账号与数据删除”的规定。用户对其个人私密数据拥有完全的自主权。无论您是在应用内还是已经卸载了应用，都可以通过以下途径彻底删除您的账号及所有云端关联数据。

### 1. 应用内即时删除路径（推荐，即刻生效）

如果您的手机上仍安装有“私密日历”应用：

1. 打开应用，进入主界面；
2. 点击右上角账号状态芯片（显示当前登录邮箱）；
3. 在弹出的账号面板中，点击底部的 **“删除账号与云端数据”**；
4. 认真阅读不可撤销警告，并选择是否保留本机离线数据（默认保留为纯离线记录，亦可选择彻底清空）；
5. 输入当前登录密码进行安全二次校验；
6. 确认后，系统将按原子事务顺序彻底删除：
   - 您名下的所有云端自慰记录集合 (`/users/{uid}/handBrewRecords/*`)；
   - 您名下的所有云端伴侣亲密记录集合 (`/users/{uid}/sexRecords/*`)；
   - 您的 Firebase Authentication 用户身份与凭据。
7. 删除完成后自动退回本机未登录状态，且云端数据不可恢复。

### 2. 网页/邮件申请删除路径（无需安装应用）

如果您已经卸载了应用、更换了设备或无法使用手机，您可以通过邮件向开发者申请删除您的云端账号与数据：

- **申请邮箱**：请发送邮件至应用支持邮箱（具体邮箱由发布负责人在 Google Play Console 声明，例如 `privacy@dailyrecord.app` 或负责人的支持邮箱）。
- **邮件标题**：`[数据删除申请] 私密日历账号及云端数据注销`
- **邮件正文**：
  - 注册并绑定的登录邮箱地址；
  - 明确表明希望彻底删除账号及云端数据的声明；
  - （可选）若需要同时清除特定备份，请说明。
- **验证与处理流程**：
  1. 为了防止恶意或冒名删除，开发者会在收到邮件后向您的注册邮箱发送一封包含验证链接或验证码的邮件；
  2. 在您确认所有权后，管理员将调用 Firebase Admin 控制台与 Cloud Functions，在 **7 个工作日内（最长不超过 30 个自然日）** 彻底清空该 UID 对应的所有 Firestore 数据集合并注销 Firebase Authentication 身份；
  3. 处理完成后，开发者将向您的邮箱发送最终确认回执。

### 3. 数据处理清单与保留政策

- **彻底删除的数据**：
  - Firebase Authentication 登录凭据（邮箱、UID、哈希密码、最后登录时间）；
  - 云端所有自慰（Personal/Solo）每日记录、次数、发生时间与感受短文本；
  - 云端所有做爱（Intimate/Duo）每日记录、次数、发生时间与感受短文本；
  - 云端同步修订号与墓碑记录。
- **保留的数据**：
  - **云端零保留**：删除完成后，云端不保留任何备份或历史快照。
  - **本机数据**：存在于您手机本地的 Room 数据库属于离线沙箱数据，网页申请无法远程擦除您的本地手机存储；如需清除本地数据，请在系统设置中“清除应用存储数据”或卸载应用。

---

## English Summary

"Private Calendar" (Daily Record) complies fully with Google Play's Account Deletion Policy. Users have complete ownership of their intimate wellness records.

### 1. In-App Deletion (Instant & Automated)

1. Open the app and tap the account status chip in the top bar.
2. Select **"Delete Account & Cloud Data"**.
3. Review the confirmation dialog and choose whether to keep local offline records.
4. Re-enter your account password for security verification.
5. All cloud documents in `/users/{uid}/handBrewRecords` and `/users/{uid}/sexRecords` along with your Firebase Authentication account are permanently deleted immediately.

### 2. Web / Email Deletion Request (Without Reinstalling)

If you have uninstalled the app or lost device access:

- **Send an email to**: The designated support email address specified in Google Play Console (e.g., `privacy@dailyrecord.app` or developer contact).
- **Subject**: `[Data Deletion Request] Private Calendar Account Removal`
- **Body**: State your registered email address and your explicit request to delete all cloud data and credentials.
- **Verification**: A verification message will be sent to your registered email to confirm account ownership.
- **Turnaround**: All cloud records and auth credentials will be permanently erased via Firebase Admin SDK within **7 business days (maximum 30 days)**, followed by an email confirmation.

### 3. Retention & Policy

- **Permanently Deleted**: Email, Firebase UID, all Solo records, all Duo records, timestamps, feelings, revisions, and tombstones.
- **Retention**: Zero cloud data is retained after deletion.
- **Local Data**: Any data residing in your device's local Room database is isolated in the Android sandbox. To erase local device data, clear app storage or uninstall the application.
