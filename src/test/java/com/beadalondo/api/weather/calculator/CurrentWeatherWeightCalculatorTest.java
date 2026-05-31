package com.beadalondo.api.weather.calculator;

import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrentWeatherWeightCalculatorTest {

    private final CurrentWeatherWeightCalculator calculator =  new CurrentWeatherWeightCalculator();

    @Test
    void rainfallScoreTest(){

        //given
        double rainfallZero = 0;
        double rainfallUnderOne = 0.5;
        double rainfallOne = 1;
        double rainfallTwo = 2;
        double rainfallThree = 3;
        double rainfallFour = 4;
        double rainfallFourteen = 14;
        double rainfallFifteen = 15;
        double rainfallTwentyNine = 29;
        double rainfallThirty = 30;

        CurrentWeatherObservation observationRainfallZero =
                new CurrentWeatherObservation(
                        0,
                        rainfallZero,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallUnderOne = new CurrentWeatherObservation(
                0,
                rainfallUnderOne,
                20,
                50,
                1
        );

        CurrentWeatherObservation observationRainfallOne =
                new CurrentWeatherObservation(
                        0,
                        rainfallOne,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallTwo =
                new CurrentWeatherObservation(
                        0,
                        rainfallTwo,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallThree =
                new CurrentWeatherObservation(
                        0,
                        rainfallThree,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallFour =
                new CurrentWeatherObservation(
                        0,
                        rainfallFour,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallFourteen =
                new CurrentWeatherObservation(
                        0,
                        rainfallFourteen,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallFifteen =
                new CurrentWeatherObservation(
                        0,
                        rainfallFifteen,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallTwentyNine =
                new CurrentWeatherObservation(
                        0,
                        rainfallTwentyNine,
                        20,
                        50,
                        1
                );

        CurrentWeatherObservation observationRainfallThirty =
                new CurrentWeatherObservation(
                        0,
                        rainfallThirty,
                        20,
                        50,
                        1
                );

        //when
        WeatherScoreResult rainfallScoreZeroResult = calculator.calculate(observationRainfallZero);
        WeatherScoreResult rainfallScoreUnderOneResult = calculator.calculate(observationRainfallUnderOne);
        WeatherScoreResult rainfallScoreOneResult = calculator.calculate(observationRainfallOne);
        WeatherScoreResult rainfallScoreTwoResult = calculator.calculate(observationRainfallTwo);
        WeatherScoreResult rainfallScoreThreeResult = calculator.calculate(observationRainfallThree);
        WeatherScoreResult rainfallScoreFourResult = calculator.calculate(observationRainfallFour);
        WeatherScoreResult rainfallScoreFourteenResult = calculator.calculate(observationRainfallFourteen);
        WeatherScoreResult rainfallScoreFifteenResult = calculator.calculate(observationRainfallFifteen);
        WeatherScoreResult rainfallScoreTwentyNineResult = calculator.calculate(observationRainfallTwentyNine);
        WeatherScoreResult rainfallScoreThirtyResult = calculator.calculate(observationRainfallThirty);

        //then
        assertEquals(0, rainfallScoreZeroResult.getWeatherScore());
        assertEquals(1, rainfallScoreUnderOneResult.getWeatherScore());
        assertEquals(2, rainfallScoreOneResult.getWeatherScore());
        assertEquals(2, rainfallScoreTwoResult.getWeatherScore());
        assertEquals(3, rainfallScoreThreeResult.getWeatherScore());
        assertEquals(3, rainfallScoreFourResult.getWeatherScore());
        assertEquals(3, rainfallScoreFourteenResult.getWeatherScore());
        assertEquals(4, rainfallScoreFifteenResult.getWeatherScore());
        assertEquals(4, rainfallScoreTwentyNineResult.getWeatherScore());
        assertEquals(5, rainfallScoreThirtyResult.getWeatherScore());
    }

    @Test
    void precipitationTypeScoreTest(){
        //given
        int zero = 0;
        int one = 1;
        int two = 2;
        int three = 3;
        int four = 4;
        int five = 5;
        int six = 6;
        int seven = 7;
        int eight = 8;

        CurrentWeatherObservation ObservationPrecipitationTypeZero = new CurrentWeatherObservation(
                zero,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeOne = new CurrentWeatherObservation(
                one,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeTwo = new CurrentWeatherObservation(
                two,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeThree = new CurrentWeatherObservation(
                three,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeFour = new CurrentWeatherObservation(
                four,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeFive = new CurrentWeatherObservation(
                five,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeSix = new CurrentWeatherObservation(
                six,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeSeven = new CurrentWeatherObservation(
                seven,
                0,
                20,
                50,
                1
        );

        CurrentWeatherObservation ObservationPrecipitationTypeEight = new CurrentWeatherObservation(
                eight,
                0,
                20,
                50,
                1
        );

        //when
        WeatherScoreResult PrecipitationTypeScoreZero =
                calculator.calculate(ObservationPrecipitationTypeZero);

        WeatherScoreResult PrecipitationTypeScoreOne =
                calculator.calculate(ObservationPrecipitationTypeOne);

        WeatherScoreResult PrecipitationTypeScoreTwo =
                calculator.calculate(ObservationPrecipitationTypeTwo);

        WeatherScoreResult PrecipitationTypeScoreThree =
                calculator.calculate(ObservationPrecipitationTypeThree);

        WeatherScoreResult PrecipitationTypeScoreFour =
                calculator.calculate(ObservationPrecipitationTypeFour);

        WeatherScoreResult PrecipitationTypeScoreFive =
                calculator.calculate(ObservationPrecipitationTypeFive);

        WeatherScoreResult PrecipitationTypeScoreSix =
                calculator.calculate(ObservationPrecipitationTypeSix);

        WeatherScoreResult PrecipitationTypeScoreSeven =
                calculator.calculate(ObservationPrecipitationTypeSeven);

        WeatherScoreResult PrecipitationTypeScoreEight =
                calculator.calculate(ObservationPrecipitationTypeEight);

        //then
        assertEquals(0,PrecipitationTypeScoreZero.getWeatherScore());
        assertEquals(0,PrecipitationTypeScoreOne.getWeatherScore());
        assertEquals(2, PrecipitationTypeScoreTwo.getWeatherScore());
        assertEquals(4, PrecipitationTypeScoreThree.getWeatherScore());
        assertEquals(0, PrecipitationTypeScoreFour.getWeatherScore());
        assertEquals(1, PrecipitationTypeScoreFive.getWeatherScore());
        assertEquals(2, PrecipitationTypeScoreSix.getWeatherScore());
        assertEquals(3, PrecipitationTypeScoreSeven.getWeatherScore());
        assertEquals(0, PrecipitationTypeScoreEight.getWeatherScore());

    }

    @Test
    void windSpeedScoreTest(){
        //given
        double zero = 0;
        double three = 3;
        double four = 4;
        double eight = 8;
        double nine = 9;
        double ten = 10;
        double thirteen = 13;
        double fourteen = 14;
        double twenty = 20;
        double twentyone = 21;
        double hundred = 100;

        CurrentWeatherObservation ObservationWindSpeedZero = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                zero
        );

        CurrentWeatherObservation ObservationWindSpeedThree = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                three
        );

        CurrentWeatherObservation ObservationWindSpeedFour = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                four
                );

        CurrentWeatherObservation ObservationWindSpeedEight = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                eight
                );

        CurrentWeatherObservation ObservationWindSpeedNine = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                nine
                );

        CurrentWeatherObservation ObservationWindSpeedTen = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                ten
                );

        CurrentWeatherObservation ObservationWindSpeedThirteen = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                thirteen
                );

        CurrentWeatherObservation ObservationWindSpeedFourteen = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                fourteen
                );

        CurrentWeatherObservation ObservationWindSpeedTwenty = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                twenty
                );

        CurrentWeatherObservation ObservationWindSpeedTwentyone = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                twentyone
                );

        CurrentWeatherObservation ObservationWindSpeedHundred = new CurrentWeatherObservation(
                0,
                0,
                20,
                50,
                hundred
                );

        //when
        WeatherScoreResult WindSpeedScoreZero =
                calculator.calculate(ObservationWindSpeedZero);

        WeatherScoreResult WindSpeedScoreThree =
                calculator.calculate(ObservationWindSpeedThree);

        WeatherScoreResult WindSpeedScoreFour =
                calculator.calculate(ObservationWindSpeedFour);

        WeatherScoreResult WindSpeedScoreEight =
                calculator.calculate(ObservationWindSpeedEight);

        WeatherScoreResult WindSpeedScoreNine =
                calculator.calculate(ObservationWindSpeedNine);

        WeatherScoreResult WindSpeedScoreTen =
                calculator.calculate(ObservationWindSpeedTen);

        WeatherScoreResult WindSpeedScoreThirteen =
                calculator.calculate(ObservationWindSpeedThirteen);

        WeatherScoreResult WindSpeedScoreFourteen =
                calculator.calculate(ObservationWindSpeedFourteen);

        WeatherScoreResult WindSpeedScoreTwenty =
                calculator.calculate(ObservationWindSpeedTwenty);

        WeatherScoreResult WindSpeedScoreTwentyone =
                calculator.calculate(ObservationWindSpeedTwentyone);

        WeatherScoreResult WindSpeedScoreHundred =
                calculator.calculate(ObservationWindSpeedHundred);

        //then
        assertEquals(0,WindSpeedScoreZero.getWeatherScore());
        assertEquals(0, WindSpeedScoreThree.getWeatherScore());
        assertEquals(1, WindSpeedScoreFour.getWeatherScore());
        assertEquals(1, WindSpeedScoreEight.getWeatherScore());
        assertEquals(2, WindSpeedScoreNine.getWeatherScore());
        assertEquals(2, WindSpeedScoreTen.getWeatherScore());
        assertEquals(2, WindSpeedScoreThirteen.getWeatherScore());
        assertEquals(4, WindSpeedScoreFourteen.getWeatherScore());
        assertEquals(4, WindSpeedScoreTwenty.getWeatherScore());
        assertEquals(5, WindSpeedScoreTwentyone.getWeatherScore());
        assertEquals(5, WindSpeedScoreHundred.getWeatherScore());
    }

    @Test
    void temperatureScoreTest(){
        //given
        double zero = 0;
        double four = 4;
        double five = 5;
        double nine = 9;
        double ten = 10;
        double twentyfour = 24;
        double twentyfive = 25;
        double hundred = 100;

        CurrentWeatherObservation ObservationTemperatureZero =
                new CurrentWeatherObservation(
                0,
                0,
                zero,
                50,
                1
        );

        CurrentWeatherObservation ObservationTemperatureFour =
                new CurrentWeatherObservation(
                        0,
                        0,
                        four,
                        50,
                        1
                );

        CurrentWeatherObservation ObservationTemperatureFive =
                new CurrentWeatherObservation(
                        0,
                        0,
                        five,
                        50,
                        1
                );

        CurrentWeatherObservation ObservationTemperatureNine =
                new CurrentWeatherObservation(
                        0,
                        0,
                        nine,
                        50,
                        1
                );

        CurrentWeatherObservation ObservationTemperatureTen =
                new CurrentWeatherObservation(
                        0,
                        0,
                        ten,
                        50,
                        1
                );

        CurrentWeatherObservation ObservationTemperatureTwentyfour =
                new CurrentWeatherObservation(
                        0,
                        0,
                        twentyfour,
                        50,
                        1
                );

        CurrentWeatherObservation ObservationTemperatureTwentyfive =
                new CurrentWeatherObservation(
                        0,
                        0,
                        twentyfive,
                        50,
                        1
                );

        CurrentWeatherObservation ObservationTemperatureHundred =
                new CurrentWeatherObservation(
                        0,
                        0,
                        hundred,
                        50,
                        1
                );

        //when
        WeatherScoreResult TemperatureScoreZero =
                calculator.calculate(ObservationTemperatureZero);

        WeatherScoreResult TemperatureScoreFour =
                calculator.calculate(ObservationTemperatureFour);

        WeatherScoreResult TemperatureScoreFive =
                calculator.calculate(ObservationTemperatureFive);

        WeatherScoreResult TemperatureScoreNine =
                calculator.calculate(ObservationTemperatureNine);

        WeatherScoreResult TemperatureScoreTen =
                calculator.calculate(ObservationTemperatureTen);

        WeatherScoreResult TemperatureScoreTwentyfour =
                calculator.calculate(ObservationTemperatureTwentyfour);

        WeatherScoreResult TemperatureScoreTwentyfive =
                calculator.calculate(ObservationTemperatureTwentyfive);

        WeatherScoreResult TemperatureScoreHundred =
                calculator.calculate(ObservationTemperatureHundred);

        //then
        assertEquals(2,TemperatureScoreZero.getWeatherScore());
        assertEquals(2,TemperatureScoreFour.getWeatherScore());
        assertEquals(1,TemperatureScoreFive.getWeatherScore());
        assertEquals(1,TemperatureScoreNine.getWeatherScore());
        assertEquals(0,TemperatureScoreTen.getWeatherScore());
        assertEquals(0,TemperatureScoreTwentyfour.getWeatherScore());
        assertEquals(0,TemperatureScoreTwentyfive.getWeatherScore());
        assertEquals(3,TemperatureScoreHundred.getWeatherScore());

    }

}
