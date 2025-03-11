//package com.tracker.demo.service;
//
//import com.tracker.demo.dto.LoadCellExerciseResult;
//import com.tracker.demo.sql.entity.LoadCellSession;
//import com.tracker.demo.sql.repository.LoadCellSessionRepository;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.testng.Assert;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//import java.time.LocalDate;
//
//import static org.mockito.Mockito.*;
//
//public class LoadCellServiceTest {
//
//    @InjectMocks
//    private LoadCellService loadCellService;
//
//    @Mock
//    private LoadCellSessionRepository mockRepository;
//
//    @BeforeMethod
//    public void setup() {
//        // Initialize @Mock and @InjectMocks fields
//        MockitoAnnotations.openMocks(this);
//
//        // Reset baseline each time to ensure a fresh test scenario
//        loadCellService.resetBaseline();
//    }
//
//    @Test
//    public void testNoSessionStartIfWeightDoesNotCrossThreshold() {
//        // We feed in negative readings that won't be 8 kg below the baseline
//        // => no session should start or end => no save on repository
//        loadCellService.processWeightData(-1.0);
//        loadCellService.processWeightData(-1.2);
//        loadCellService.processWeightData(-2.0);
//        loadCellService.processWeightData(-0.5);
//
//        verify(mockRepository, never()).save(any(LoadCellSession.class));
//    }
//
//    @Test
//    public void testSessionStartAndStopAtCrossThreshold() {
//        // 1) Feed multiple idle readings to stabilize baseline near ~-1
//        //    so that a single -10 reading won't drastically pull the baseline down
//        for (int i = 0; i < 8; i++) {
//            loadCellService.processWeightData(-1.0);
//        }
//        // At this point, baseline is near -1.
//
//        // 2) Now feed -10 => that should be well below baseline - 8 (e.g. ~-9)
//        //    => triggers exercise start
//        loadCellService.processWeightData(-10.0);
//
//        // 3) Then feed a reading close to -1 => triggers exercise stop
//        loadCellService.processWeightData(-1.2);
//
//        ArgumentCaptor<LoadCellSession> captor = ArgumentCaptor.forClass(LoadCellSession.class);
//        verify(mockRepository, times(1)).save(captor.capture());
//
//        LoadCellSession savedSession = captor.getValue();
//        Assert.assertNotNull(savedSession, "Session should have been saved");
//        Assert.assertTrue(savedSession.getDurationSeconds() > 0,
//                "Duration should be greater than zero after a real session");
//        // By default, the goal is 300 in LoadCellService
//        Assert.assertEquals(savedSession.getGoal(), 300L,
//                "Goal should be the default 300 seconds");
//        Assert.assertTrue(savedSession.getPercentage() > 0f,
//                "Percentage should be > 0 if we had a real session");
//    }
//
//    @Test
//    public void testMultipleConsecutiveReadingsBelowThreshold() {
//        // Stabilize baseline near -2
//        loadCellService.processWeightData(-2.0);
//        loadCellService.processWeightData(-1.8);
//        loadCellService.processWeightData(-2.2);
//
//        // Now feed two consecutive readings that are ~8 kg below baseline
//        // => definitely triggers start
//        loadCellService.processWeightData(-11.0);
//        loadCellService.processWeightData(-12.0);
//
//        // Confirm a stop with a reading near -1
//        loadCellService.processWeightData(-1.0);
//
//        verify(mockRepository, times(1)).save(any(LoadCellSession.class));
//    }
//
//    @Test
//    public void testGetTotalExerciseTimeToday_WhenNoSession() {
//        when(mockRepository.findByDate(LocalDate.now())).thenReturn(null);
//
//        LoadCellExerciseResult result = loadCellService.getTotalExerciseTimeToday();
//        // Adjust these checks if your getters use different names or return types:
//        // e.g., if getSecondsPracticed() is a long, compare to 0L:
//        Assert.assertEquals(result.getSecondsPracticed(), 0L,
//                "No session => 0 seconds practiced");
//        Assert.assertEquals(result.getGoalSeconds(), 300L,
//                "Default goal if no session found");
//        Assert.assertEquals(result.getPercentage(), 0.0f,
//                "Percentage should be 0 if no session found");
//    }
//
//    @Test
//    public void testGetTotalExerciseTimeToday_WithSession() {
//        // Suppose the DB has a record for today with 120 seconds, 300 goal, 40% done
//        LoadCellSession session = new LoadCellSession();
//        session.setDate(LocalDate.now());
//        session.setDurationSeconds(120L);
//        session.setGoal(300L);
//        session.setPercentage(40f);
//
//        when(mockRepository.findByDate(LocalDate.now())).thenReturn(session);
//
//        LoadCellExerciseResult result = loadCellService.getTotalExerciseTimeToday();
//        Assert.assertEquals(result.getSecondsPracticed(), 120L,
//                "Should reflect the session's stored duration");
//        Assert.assertEquals(result.getGoalSeconds(), 300L,
//                "Should reflect the session's stored goal");
//        Assert.assertEquals(result.getPercentage(), 40f,
//                "Should reflect the session's stored percentage");
//    }
//}
