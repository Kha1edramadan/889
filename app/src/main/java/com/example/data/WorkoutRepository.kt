package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutDao) {

    val allPlans: Flow<List<WorkoutPlan>> = dao.getAllPlans()
    val allExercises: Flow<List<Exercise>> = dao.getAllExercises()
    val recentSessions: Flow<List<WorkoutSession>> = dao.getRecentSessions()

    fun getExercisesForPlan(planId: String): Flow<List<Exercise>> =
        dao.getExercisesForPlan(planId)

    // ─── Set Logs ─────────────────────────────────────────────────────────────

    suspend fun logSet(setLog: SetLog) = dao.insertSetLog(setLog)
    suspend fun deleteSetLog(id: Int) = dao.deleteSetLog(id)

    fun getLogsForExercise(exerciseId: String): Flow<List<SetLog>> =
        dao.getLogsForExercise(exerciseId)

    fun getLastSessionLogsForExercise(exerciseId: String): Flow<List<SetLog>> =
        dao.getLastSessionLogsForExercise(exerciseId)

    fun getRecentSessionLogsForExercise(exerciseId: String): Flow<List<SetLog>> =
        dao.getRecentSessionLogsForExercise(exerciseId)

    // ─── Workout Sessions ─────────────────────────────────────────────────────

    suspend fun recordWorkoutSession(session: WorkoutSession) =
        dao.insertWorkoutSession(session)

    suspend fun deleteWorkoutSession(sessionId: Long) =
        dao.deleteWorkoutSession(sessionId)

    fun getSessionsSince(since: Long): Flow<List<WorkoutSession>> =
        dao.getSessionsSince(since)

    fun getSessionCountSince(since: Long): Flow<Int> =
        dao.getSessionCountSince(since)

    fun getTotalSessionCount(): Flow<Int> =
        dao.getTotalSessionCount()

    // ─── Nutrition Logs ───────────────────────────────────────────────────────

    suspend fun logNutrition(log: NutritionLog): Long =
        dao.insertNutritionLog(log)

    suspend fun removeNutritionLog(id: Int) =
        dao.deleteNutritionLog(id)

    suspend fun editNutritionLog(id: Int, grams: Float, calories: Int, protein: Float, carbs: Float, fat: Float) =
        dao.updateNutritionLog(id, grams, calories, protein, carbs, fat)

    fun getNutritionLogsForDay(dayKey: Long): Flow<List<NutritionLog>> =
        dao.getNutritionLogsForDay(dayKey)

    // ─── Body Weight ──────────────────────────────────────────────────────────

    suspend fun logBodyWeight(log: BodyWeightLog) =
        dao.insertBodyWeightLog(log)

    suspend fun deleteBodyWeightLog(id: Int) =
        dao.deleteBodyWeightLog(id)

    fun getRecentBodyWeightLogs(): Flow<List<BodyWeightLog>> =
        dao.getRecentBodyWeightLogs()

    // ─── Personal Records ─────────────────────────────────────────────────────

    fun epley(weight: Float, reps: Int): Float =
        if (reps <= 1) weight else weight * (1f + reps / 30f)

    suspend fun checkAndSavePR(
        exerciseId: String,
        exerciseName: String,
        exerciseNameAr: String,
        weightKg: Float,
        reps: Int
    ): Boolean {
        val orm = epley(weightKg, reps)
        val existing = dao.getPersonalRecord(exerciseId)
        return if (existing == null || orm > existing.estimatedOneRepMax) {
            dao.upsertPersonalRecord(
                PersonalRecord(exerciseId, exerciseName, exerciseNameAr, weightKg, reps, orm)
            )
            true
        } else false
    }

    suspend fun deletePersonalRecord(exerciseId: String) =
        dao.deletePersonalRecord(exerciseId)

    fun getAllPersonalRecords(): Flow<List<PersonalRecord>> = dao.getAllPersonalRecords()

    // ─── Water Tracking ───────────────────────────────────────────────────────

    fun getTodayWaterMl(dayKey: Long): Flow<Int> = dao.getTodayWaterMl(dayKey)

    suspend fun addWater(dayKey: Long, amountMl: Int) =
        dao.insertWaterLog(WaterLog(dayKey = dayKey, amountMl = amountMl))

    suspend fun resetTodayWater(dayKey: Long) = dao.clearTodayWater(dayKey)

    // ─── Body Measurements ────────────────────────────────────────────────────

    suspend fun insertBodyMeasurement(m: BodyMeasurement) = dao.insertBodyMeasurement(m)

    suspend fun deleteBodyMeasurement(id: Int) = dao.deleteBodyMeasurement(id)

    fun getRecentBodyMeasurements(): Flow<List<BodyMeasurement>> = dao.getRecentBodyMeasurements()

    // ─── Workout Notes ────────────────────────────────────────────────────────

    suspend fun saveWorkoutNote(sessionId: Long, note: String) =
        dao.insertWorkoutNote(WorkoutNote(sessionId = sessionId, note = note))

    suspend fun getNoteForSession(sessionId: Long): WorkoutNote? =
        dao.getNoteForSession(sessionId)

    // ─── RPE ──────────────────────────────────────────────────────────────────

    suspend fun logRpe(sessionId: Long, exerciseId: String, setNumber: Int, rpe: Int) =
        dao.insertRpeLog(RpeLog(sessionId = sessionId, exerciseId = exerciseId, setNumber = setNumber, rpeValue = rpe))

    // ─── Custom Meals ─────────────────────────────────────────────────────────

    suspend fun insertCustomMeal(meal: CustomMeal) = dao.insertCustomMeal(meal)

    fun getAllCustomMeals(): Flow<List<CustomMeal>> = dao.getAllCustomMeals()

    suspend fun deleteCustomMeal(id: Int) = dao.deleteCustomMeal(id)

    // ─── Seed Data ────────────────────────────────────────────────────────────
    // تشغّل مرة واحدة عند أول فتح — REPLACE بيحدث التغييرات الجديدة تلقائياً.
    // التمارين المتنقلة بين الخطط: ub4 → Lower | ub6 → Shoulders | pc2 → Lower.
    // التمارين المحذوفة: pc4 (Cable Pull-Through) | pc5 (Seated Cable Row من Posture).

    suspend fun populateInitialData() {

        val upperPlan    = WorkoutPlan("plan_upper",   "UPPER",            "UPPER",            1)
        val lowerPlan    = WorkoutPlan("plan_lower",   "LOWER",            "LOWER",            2)
        val chestBackPlan= WorkoutPlan("plan_cb",      "CHEST & BACK",     "CHEST & BACK",     3)
        val shouldersPlan= WorkoutPlan("plan_sa",      "SHOULDERS & ARMS", "SHOULDERS & ARMS", 4)
        val posturePlan  = WorkoutPlan("plan_posture", "POSTURE & CORE",   "POSTURE & CORE",   5)
        dao.insertPlans(listOf(upperPlan, lowerPlan, chestBackPlan, shouldersPlan, posturePlan))

        val exercises = listOf(

            // ══════════════════════════════════════════════════════════
            // UPPER  (5 تمارين — ub4 انتقل لـ Lower، ub6 انتقل لـ S&A)
            // ══════════════════════════════════════════════════════════
            Exercise("ub1","plan_upper",1,"Machine Chest Press","بريس الصدر بالجهاز",2,"6-8","1-2","3-5 دقائق","الصدر",
                "اضبط الكرسي تكون المقابض بمستوى منتصف صدرك\n• ثبّت ظهرك ولوحي كتفك في الكرسي",
                "ادفع الوزن بثبات بدون ما تفرد كوعك للآخر\n• ارجع بالوزن ببطء وتحكم",
                "1. دمبل مسطح (مدى حركي أعمق)\n2. بار مسطح حر (أثقل وزن)\n3. كيبل كروس منتصف (عزل أفضل)"),

            Exercise("ub2","plan_upper",2,"T-Bar Row","تجديف تي-بار",1,"5-7","1-2","3-5 دقائق","الظهر",
                "اسحب الوزن ناحية بطنك مش صدرك\n• حافظ على استقامة أسفل ظهرك تماماً",
                "ركز في السحب من كوعك كأنه خطاف\n• اعصر عضلات ظهرك في أعلى نقطة",
                "1. تجديف دمبل أحادي (عزل ومدى أعمق)\n2. تجديف بار بنط\n3. سحب أرضي بالكيبل"),

            Exercise("ub3","plan_upper",3,"Incline Machine Press","بريس الصدر المائل بالجهاز",1,"6-10","1-2","3-5 دقائق","الصدر",
                "ركز على الدفع من الصدر العلوي مش من إيدك\n• انزل بالوزن ببطء لاستطالة العضلة",
                "المدى الحركي الكامل أهم من وزن تقيل\n• ثبّت قدميك في الأرض بقوة",
                "1. دمبل مائل 30° (نفس الألياف)\n2. كيبل كروس من أسفل للأعلى\n3. بار مائل حر (للمتقدمين)"),

            Exercise("ub5","plan_upper",4,"Cable Bicep Curl","كيرل البايسبس بالكيبل",1,"6-10","0","3-5 دقائق","البايسبس",
                "قف مستقيم وثبّت كوعك بجانب جسمك\n• ارفع الوزن بتركيز بدون ما تحرك كتفك",
                "النزول ببطء بيبني العضلة بشكل ممتاز\n• متستخدمش وزن يجعل جسمك يتأرجح",
                "1. كيرل دمبل تبادلي\n2. كيرل بار بنط (أريح للرسغ)\n3. كيرل دمبل مائل 45° (استطالة أكبر)"),

            Exercise("ub7","plan_upper",5,"Cable Shrugs","شراغ الكيبل",1,"6-8","1","3-5 دقائق","الترابيس",
                "ارفع كتفك في خط مستقيم ناحية أذنك\n• اعصر العضلة ثانية في الأعلى",
                "تجنب الدوران بالكتف لحماية مفاصل رقبتك\n• انزل بالوزن ببطء شديد",
                "1. شراغز دمبل (مدى حركي أوسع)\n2. شراغز بار (أثقل وزن)\n3. شراغز جهاز سميث"),

            // ══════════════════════════════════════════════════════════
            // LOWER  (9 تمارين — ub4 انتقل هنا + hip thrust من Posture)
            // ══════════════════════════════════════════════════════════
            Exercise("lo1","plan_lower",1,"Machine Lateral Raise","رفرفة الكتف الجانبي على الجهاز",2,"6-8","1-2","3-5 دقائق","الكتف",
                "حاول الحركة تيجي من كتفك لوحده مش من جسمك كله",
                "النزول البطيء سر التمرين — متسقطش الوزن بسرعة",
                "1. Cable Lateral Raises\n2. DB Lateral Raises"),

            Exercise("lo2","plan_lower",2,"Leg Press Calf Raise","رفعات السمانة بجهاز الليج بريس",2,"5-7","1-2","3-5 دقائق","السمانة",
                "ادفع بمشط قدمك ببطء للأعلى\n• اثبت في الأعلى ثانية واكمل العصر",
                "النزول لأقصى استطالة في الأسفل هو سر تطور السمانة",
                "1. سمانة جالس بالجهاز (عزل أفضل)\n2. سمانة واقف بالدمبل أحادي\n3. سمانة جهاز سميث"),

            Exercise("lo3","plan_lower",3,"Hack Squat","هاك سكوات",1,"5-8","1-3","3-5 دقائق","الأرجل",
                "120 درجة من ثني الركبة تكفي لاستهداف الكوادز\n• بس حاول تنزل للآخر",
                "متفردش ركبتك بالكامل في الأعلى للحماية\n• ثبّت أسفل ظهرك كاملاً على المسند",
                "1. Smith Squat\n2. Leg Press"),

            Exercise("lo4","plan_lower",4,"Prone Leg Curl","كيرل الأرجل المستلقي",1,"8-12","1-2","3-5 دقائق","الأرجل",
                "اسحب كعبيك للأعلى نحو جسمك بقوة واعصر في الأعلى\n• ارجع ببطء لأقصى استطالة",
                "لو الجهاز مش موجود العب SLDL",
                "1. Seated Leg Curl\n2. SLDL"),

            Exercise("lo5","plan_lower",5,"Leg Extension","مد الأرجل",1,"8-12","1-2","3-5 دقائق","الأرجل",
                "افرد رجلك للأعلى بثبات وتوقف جزء من الثانية\n• النزول البطيء يقوي الأربطة",
                "لو الجهاز مش موجود العب BANDED LEG EXTENSION",
                "1. Banded Leg Extension\n2. Leg Press بوضع قدم ضيق"),

            Exercise("lo6","plan_lower",6,"Hip Adductor Machine","الضامة",1,"6-8","1-2","3-5 دقائق","الأرجل",
                "ثبّت نفسك بالمقبض كويس ومتحركش على الجهاز",
                "الحركة بطيئة وتحكم — تجنب قذف الوزن لإغلاق الجهاز",
                "1. Cable Hip Adduction\n2. Copenhagen Adductor"),

            Exercise("lo7","plan_lower",7,"Wrist Curl","كيرل الرسغ",1,"6-10","0","3-5 دقائق","السواعد",
                "فط صوابعك تحت في الاستطالة\n• اختار وزن يخليك تعمل انقباض كامل",
                "الحركة من المعصم فقط — ثبّت ساعدك بالكامل على الدكة",
                "1. Reverse Wrist Curl\n2. كيرل بار مقلوب"),

            // ub4: Single Arm — انتقل من Upper (seq 4) لـ Lower (seq 8)
            Exercise("ub4","plan_lower",8,"Single Arm Lat Pulldown","سحب اللات أحادي",1,"6-10","0","3-5 دقائق","الظهر",
                "بلاش تتلف حوالين نفسك\n• ثبّت جسمك طول الحركة — الهدف سحب الكوع للأسفل بس",
                "التحكم في الرجوع (Eccentric) مهم جداً\n• متسيبش الوزن يطلع لفوق بسرعة",
                "1. Cable SA Lat Row\n2. DB SA Lat Row"),

            // pc2: Hip Thrust — انتقل من Posture (seq 2) لـ Lower (seq 9)
            Exercise("pc2","plan_lower",9,"Hip Thrust","هيب ثراست",3,"10-12","1","3-5 دقائق","الأرجل",
                "ثبّت كتفيك على الدكة وادفع من كعبيك للأعلى\n• في الأعلى اعصر عضلات الجلوس بقوة وخلي ظهرك مستوي في الهواء",
                "القعدة الطويلة بتخمّد عضلات الجلوس خالص، وده التمرين اللي بيوقف الضرر ده\n• لو ركبتك بتوجعك ادفع من الكعب مش من أصابع القدم",
                "1. Glute Bridge أرضي (أسهل)\n2. Cable Kickback (عزل أحسن)\n3. ديدليفت روماني RDL"),

            // ══════════════════════════════════════════════════════════
            // CHEST & BACK  (6 تمارين — مفيش تغيير)
            // ══════════════════════════════════════════════════════════
            Exercise("ua1","plan_cb",1,"Smith High Incline Press","بريس المائل العالي بجهاز سميث",2,"4-6","1-2","3-5 دقائق","الصدر",
                "ضم إيدك لجوه عشان تحاكي اتجاه ألياف الصدر العالي\n• بلاش تفتح كوعك 90 درجة",
                "ركز على الضغط من الصدر العلوي مش من إيدك\n• انزل ببطء وتحكم حتى البار يلامس أعلى الصدر",
                "1. دمبل مائل عالي DB High Incline Press\n2. كيبل كروس من أسفل للأعلى"),

            Exercise("ua2","plan_cb",2,"Machine Wide Grip Lat Pulldown","سحب اللات العريض على الجهاز",2,"6-8","1-3","3-5 دقائق","الظهر",
                "ركز في مسار كوعك وإنك بتضم كتافك على بعض\n• مش بتسحب على ظهرك العلوي",
                "التحكم في الرجوع (Eccentric) مهم جداً — متسيبش الوزن يطلع بسرعة",
                "1. Cable Wide Grip Lat Pulldown\n2. عقلة واسعة (الأصعب والأفضل)"),

            Exercise("ua3","plan_cb",3,"Chest Press Machine","بريس الصدر بالجهاز",1,"6-10","1-2","3-5 دقائق","الصدر",
                "اجعل لوحي كتفك مشدودين للخلف وثبتهما بالكرسي\n• ادفع الوزن وتجنب فرد الكوع لآخره",
                "الأداء البطيء يضاعف تفعيل ألياف الصدر\n• حافظ على صدرك مرفوعاً",
                "1. دمبل مسطح (مدى أعمق)\n2. بار مسطح حر\n3. غطس بوزن Dips"),

            Exercise("ua4","plan_cb",4,"T-Bar Row","تجديف تي-بار",1,"5-7","1-2","3-5 دقائق","الظهر",
                "افتح كيعانك لبره على قدر ما تقدر\n• حاول تقرب من زاوية 90 درجة",
                "الهدف سماكة الظهر — اقبض عضلات الظهر في الأعلى\n• خلي ظهرك مستقيم طول الوقت",
                "1. Incline DB Row\n2. Cable Row"),

            Exercise("ua5","plan_cb",5,"Cable Shrugs","شراغ الترابيس بالكيبل",1,"6-8","1","3-5 دقائق","الترابيس",
                "شد الكيبل من أسفل وارفع كتفيك مستقيماً ناحية أذنيك — الحركة عمودية بحتة\n• اعصر الترابيس في الأعلى ثانية كاملة",
                "تجنب الدوران بالكتف — الحركة رفع ونزول فقط\n• النزول البطيء يضاعف تفعيل العضلة",
                "1. شراغ دمبل (مدى حركي أوسع)\n2. شراغ بار (أثقل وزن)\n3. شراغ جهاز سميث"),

            Exercise("ua6","plan_cb",6,"Seated Cable Row","تجديف الكيبل الأرضي",1,"6-10","0","3-5 دقائق","الظهر",
                "اجلس منتصباً واسحب نحو معدتك مع سحب لوحي كتفك للخلف\n• اثبّت ثانية واعصر عضلات الظهر المتوسطة",
                "الجذع ثابت طوال الوقت\n• الحركة تبدأ من سحب الكوع للخلف مش من الأكتاف",
                "1. تجديف دمبل أحادي\n2. T-Bar Row (سماكة أكثر)\n3. جهاز الروينج"),

            // ══════════════════════════════════════════════════════════
            // SHOULDERS & ARMS  (7 تمارين — ub6 انتقل هنا كأول تمرين)
            // ══════════════════════════════════════════════════════════

            // ub6: Overhead Tricep Ext — انتقل من Upper (seq 6) لـ S&A (seq 1)
            Exercise("ub6","plan_sa",1,"Tricep Overhead Extension","مد الترايسبس فوق الرأس",2,"6-10","0","3-5 دقائق","الترايسبس",
                "ثبّت كوعك بجوار رأسك قدر الإمكان\n• انزل للأسفل للحصول على أقصى استطالة",
                "الاستطالة في الأسفل هي اللي بتبني الترايسبس حقيقي\n• لو كوعك وجعك العب Push Down عادي",
                "1. DB Skull Crusher\n2. كيبل أوفرهيد\n3. EZ Bar Overhead"),

            Exercise("lb1","plan_sa",2,"SA Tricep Pushdown","بوش داون الترايسبس كيبل أحادي",2,"6-10","0","3-5 دقائق","الترايسبس",
                "متدخلش الكور بزيادة ومتتمرجحش\n• هي فرد للكوع فقط",
                "استخدم وزن يخليك تتحكم في الحركة بالكامل\n• ارجع للأعلى ببطء",
                "1. Double Rope Pushdown\n2. V-Bar Pushdown"),

            Exercise("lb2","plan_sa",3,"Cable Lateral Raise","رفرفة الكتف الجانبي بالكيبل",2,"5-8","1-2","3-5 دقائق","الكتف",
                "اسحب الكيبل من مستوى الكاحل بمحاذاة كتفك\n• الجسم ثابت والحركة كلها من الكتف",
                "النزول البطيء يعطي نتائج أفضل بكثير من أوزان تقيلة بمرجحة\n• ارفع لمستوى الكتف بالضبط",
                "1. رفرفة جانبي دمبل جالس\n2. جهاز الكتف الجانبي\n3. رفرفة كيبل جالس"),

            Exercise("lb3","plan_sa",4,"Seated DB Bicep Curl","كيرل البايسبس دمبل جالس على الدكة",2,"6-10","0","3-5 دقائق","البايسبس",
                "بلاش مدى حركة زيادة من الكتف\n• ومتتمرجحش — الحركة كلها من كوعك",
                "ارجع بالوزن ببطء (Eccentric) — ده اللي بيبني البايسبس بجد",
                "1. Face Away Curl\n2. DB Curls واقف"),

            Exercise("lb4","plan_sa",5,"Forearm Cable Curl","كيرل السواعد بالكيبل",1,"6-10","0","3-5 دقائق","السواعد",
                "تقدر تغيّر القبضات على حسب ما يريحك\n• المهم نفس الأداء",
                "فط صوابعك تحت في الاستطالة\n• اختار وزن يخليك تعمل انقباض كامل",
                "1. كيرل رسغ بالبار جالس\n2. Reverse Curl"),

            Exercise("lb5","plan_sa",6,"Rear Delt Butterfly","رفرفة الكتف الخلفي على جهاز الفراشة",1,"6-10","0","3-5 دقائق","الكتف",
                "فك لوحين كتفك قبل ما تبدأ الحركة\n• ركز على الكتف الخلفي مش على الظهر",
                "الوزن الخفيف مع التركيز العالي أفضل بكثير\n• اعكس جلستك على الجهاز",
                "1. Reverse Pec Dec\n2. Face Pulls بالكيبل"),

            Exercise("lb6","plan_sa",7,"Front Raise Machine","رفع الكتف الأمامي على جهاز السحب لفوق",1,"6-10","0","3-5 دقائق","الكتف",
                "استخدم ذراع جهاز السحب لفوق وارفع بشكل أمامي نحو مستوى كتفك\n• الحركة من الكتف الأمامي بتحكم",
                "متكملش فوق مستوى الكتف لتجنب الضغط على المفصل\n• النزول البطيء يفرق كتير",
                "1. رفع أمامي دمبل\n2. رفع أمامي كيبل (شد مستمر)\n3. رفع أمامي بار"),

            // ══════════════════════════════════════════════════════════
            // POSTURE & CORE  (5 تمارين مبسطة — بدون pc4 وpc5 وpc2)
            // ══════════════════════════════════════════════════════════
            Exercise("pc1","plan_posture",1,"Face Pulls","فيس بولز — كتف خلفي وثبات الكتف",3,"12-15","0","60-90 ثانية","الكتف",
                "اسحب الحبل نحو وجهك وافتح كوعيك للجانبين وللأعلى في نفس الوقت\n• اعصر الكتف الخلفي ثانية في النهاية",
                "ده التمرين الأهم لمواجهة أثر القعدة الطويلة، بيفتح الكتف اللي بيتلف للأمام\n• وزن خفيف وتركيز عالي أفضل من وزن تقيل",
                "1. رفرفة خلفي بالدمبل جالساً\n2. Band Pull-Apart يومياً بدون جهاز\n3. فراشة خلفي على الجهاز"),

            Exercise("pc3","plan_posture",2,"Dead Bug","ديد باج — ثبات الكور",3,"8-10","0","60 ثانية","البطن",
                "انقر ظهرك بالأرض تماماً ومتحركش طول الوقت\n• مد ذراع وساق في الجهتين المتقابلين ببطء وارجع",
                "أفضل تمرين علمياً للكور من غير أي ضغط على الفقرات\n• لو حسيت ظهرك رفع ولو مليمتر — صغّر الحركة",
                "1. Bird Dog على الأربعة\n2. Hollow Body Hold (أصعب نسخة)\n3. بلانك مع رفع ذراع وساق"),

            // تمرين البطن بالكيبل الجديد (طلب المستخدم)
            Exercise("pc_cab","plan_posture",3,"Cable Crunch","ضغط البطن بالكيبل",3,"12-15","0","60 ثانية","البطن",
                "اركع على الأرض ومسك الكيبل خلف رأسك أو على صدرك\n• انحن من الظهر للأسفل وحس بالانقباض في عضلات بطنك",
                "الحركة من الظهر مش من الكتف — ركز تحس بالبطن بتنضغط\n• استطالة كاملة في الأعلى وانقباض قوي في الأسفل",
                "1. Decline Crunch بوزن\n2. Ab Wheel Rollout (أصعب)\n3. Hanging Knee Raise"),

            Exercise("pc6","plan_posture",4,"Thoracic Extension","فرد الظهر العلوي — علاج الانحناء",2,"8-10","2","60 ثانية","الظهر",
                "حط الفوم رولر أو حافة الدكة تحت الجزء الأوسط من ظهرك\n• انبسط للخلف ببطء وإيديك خلف دماغك وحس بالفتح في صدرك",
                "العمود الفقري الصدري بيتصلّب من القعدة الطويلة، وده التمرين بيفتحه مباشرة\n• تقدر تعمله كل يوم وقبل أي تمرين كإحماء",
                "1. تمديد الصدر على حافة الدكة\n2. Cat-Cow يومياً\n3. Door Frame Chest Stretch"),

            Exercise("pc7","plan_posture",5,"Hip Flexor Stretch","تمديد أمام الفخذ — تخفيف ألم الظهر",2,"30-45ث","0","30 ثانية","الأرجل",
                "اركع وحرّك حوضك للخلف (تخيل زنار بيشدك من تحت) أثناء التمديد — ده سر فاعليته\n• 30-45 ثانية على كل جهة مع تنفس بطيء عميق",
                "عضلات أمام الفخذ الأكثر تضرراً من القعدة الطويلة، تقصيرها بيعمل ألم أسفل الظهر\n• متحاولش تتمدد بالقوة — خلي الجسم يسيب ببطء",
                "1. Couch Stretch (أقوى نسخة)\n2. Pigeon Pose\n3. Hip Flexor Stretch بالكيبل واقفاً")
        )
        dao.insertExercises(exercises)

        // تنظيف التمارين اللي اتشالت من Posture بالكامل
        // (pc4 = Cable Pull-Through, pc5 = Seated Cable Row من Posture — اتشالوا)
        dao.deleteExercisesByIds(listOf("pc4", "pc5"))
    }
}
