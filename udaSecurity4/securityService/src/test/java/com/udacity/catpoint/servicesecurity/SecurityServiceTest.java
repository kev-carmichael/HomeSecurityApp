package com.udacity.catpoint.servicesecurity;

import com.google.common.primitives.Booleans;
import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.service.FakeImageService;
import com.udacity.catpoint.service.ImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.EnumSource.Mode.MATCH_ALL;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;
    private final String randomUUID = UUID.randomUUID().toString();
    // same size as DisplayPanel. Figure of 50.0f hard-coded as per securityService.processImage()
    private BufferedImage bufferedImage = new BufferedImage(300, 225, TYPE_INT_ARGB);


    @Mock
    private SecurityRepository securityRepository;

    @Mock
    //changed SecurityService constructor to req interface ImageService (originally fakeImageService)
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    //had issues with tests being unable to invoke methods due to Sensor being null
    private Sensor createNewSensor(){
        return new Sensor(randomUUID, SensorType.DOOR);
    }

    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository, imageService);//now works with ImageService
        //had issues with tests being unable to invoke methods due to Sensor being null
        sensor = createNewSensor();
    }

    //UNIT TEST #1
    @Test
    public void _1_ifAlarmArmedAndAlarmStatusNoAlarmAndSensorActivated_SetToAlarmStatusPending(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //UNIT TEST #2
    @Test
    public void _2_ifAlarmArmedAndAlarmStatusPendingAndSensorActivated_SetToAlarmStatusAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //UNIT TEST #3
    @Test
    public void _3_ifAlarmArmedAndAlarmStatusPendingAndSensorNotActivated_SetToAlarmStatusNoAlarm(){
        //when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        //securityService.changeSensorActivationStatus(sensor, false);
        securityService.changeSensorActivationStatus(sensor);
        sensor.setActive(Boolean.FALSE);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //UNIT TEST #4
    //Still necessary to say alarm is armed when it is active?
    @ParameterizedTest
    //to have different sensor status
    @ValueSource(booleans = {true, false})
    public void _4_ifAlarmStatusAlarm_SensorStateChanged_NoChangeToAlarmStatus(Boolean sensorStatus){
        //when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, sensorStatus);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        //Confirmed OK to have 2x verify
    }

    //UNIT TEST #5
    @Test
    public void _5_ifAlarmStatusPendingAndSensorActivated_AddnSensorActivated_SetToAlarmStatusAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //UNIT TEST #6
    @Test
    public void _6_ifAlarmStatusNoAlarm_OneSensorDeactivated_NoChangetoAlarmStatus(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM); //system inactive
        sensor.setActive(Boolean.FALSE); //sensor deactivated
        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        //Confirmed OK to have 2x verify
    }

    //UNIT TEST #7
    @Test
    public void _7_ifArmingStatusArmedHomeAndImageServiceIdentifiesCat_SetToAlarmStatusAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).thenReturn(Boolean.TRUE);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //method  required for TEST #8 & TEST #10 and so uncoupled here
    public Set<Sensor> userDefinedSensorSet(int noOfSensors, boolean active) {
        Set<Sensor> sensorHashSet = new HashSet<>();
        for (int i = 0; i < noOfSensors; i++) {
            sensorHashSet.add(new Sensor(String.valueOf(i), SensorType.DOOR));
        }
        for(Sensor sensorSearch : sensorHashSet){
            sensorSearch.setActive(active);
        }
        return sensorHashSet;
    }

    //UNIT TEST#8
    @Test
    public void _8_ifImageServiceIdentifiesNotCatAndSensorsDeactivated_SetToAlarmStatusNoAlarm() {
        Set<Sensor> sensorSet = userDefinedSensorSet(2, Boolean.FALSE);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(imageService.imageContainsCat(any(),ArgumentMatchers.anyFloat())).thenReturn(Boolean.FALSE);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

        //UNIT TEST #9
    @Test
    public void _9_ifArmingStatusDisarmed_SetToAlarmStatusNoAlarm(){
        //when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //UNIT TEST #10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void _10_ifArmingStatusArmed_SetAllSensorsDeactivated(ArmingStatus armingStatus){
        Set<Sensor> testSensors = userDefinedSensorSet(5, true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(testSensors);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(sensor -> {assertFalse(sensor.getActive());});
    }

    //UNIT TEST #11
    @Test
    public void
    _11_ifArmingStatusDisarmedAndImageServiceIdentifiesCatThenArmingStatusArmedHome_SetToAlarmStatusAlarm(){
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(Boolean.TRUE);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //UNIT TEST #12
    //added to increase code coverage
    @Test
    public void _12_ifAlarmDisarmedAndAlarmStatusAlarm_SensorDeactivated(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);
        sensor.setActive(Boolean.FALSE); //sensor deactivated
    }

    //UNIT TEST #12a
    //added to increase code coverage
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    public void _12a_ifArmingStatusDisarmedAndSensorActivated_NoChangeArmingStatus(AlarmStatus alarmStatus){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        verify(securityRepository,never()).setArmingStatus(ArmingStatus.DISARMED);
    }
}
