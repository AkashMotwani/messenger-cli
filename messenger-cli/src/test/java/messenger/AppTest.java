/*
 * Copyright 2015 Dirk Franssen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package messenger;

import com.ditavision.messengerengine.MessengerEngine;
import com.ditavision.messengerengine.MessengerEngineException;
import com.ditavision.messengerengine.mmp.response.MMPRecipientStatus;
import com.ditavision.messengerengine.mmp.response.MMPStatusReportDetail;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author dfranssen
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({App.class})
public class AppTest {
    
    SystemExitHelper mockExitHelper;
    
    @Before
    public void setup() throws Exception {
        mockExitHelper = mock(SystemExitHelper.class);
        doNothing().when(mockExitHelper).exit(anyInt());
        PowerMockito.whenNew(SystemExitHelper.class).withNoArguments().thenReturn(mockExitHelper);
    }
    
    @Test
    public void splitNull() {
        List<String> actual = App.split(null);
        assertThat(actual, nullValue());
    }
    
    @Test
    public void splitOne() {
        List<String> actual = App.split("a;b");
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0), is("a;b"));
    }
    
    @Test
    public void splitMore() {
        List<String> actual = App.split("a,b");
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0), is("a"));
        assertThat(actual.get(1), is("b"));
    }
    
    @Test
    public void getValueOriginal() {
        String actual = App.getValue("OK", null, null);
        assertThat(actual, is("OK"));
    }

    @Test
    public void getValueDefault() {
        String actual = App.getValue(null, "OK", null);
        assertThat(actual, is("OK"));
    }

    @Test
    public void getValueSystemIn() throws Exception {
        PrintStream stream = Mockito.mock(PrintStream.class);
        BufferedReader mockBR = configureInput("OK");
        System.setOut(stream);
        String actual = App.getValue(null, null, "test");
        verify(stream).printf(anyString(), eq("test"));
        verify(mockBR).readLine();
        assertThat(actual, is("OK"));
    }

    @Test
    public void nullOrEmptyWithNull() {
        boolean actual = App.nullOrEmpty(null);
        assertThat(actual, is(true));
    }

    @Test
    public void nullOrEmptyWithEmpty() {
        boolean actual = App.nullOrEmpty("");
        assertThat(actual, is(true));
    }

    @Test
    public void nullOrEmptyWithValue() {
        boolean actual = App.nullOrEmpty("OK");
        assertThat(actual, is(false));
    }

    @Test
    public void arrayToMapIgnoreLast() {
        String[] args = {"--init", "-u", "me", "--status", "-h", "ingoreMe"};
        Map<String, String> argumentMap = App.arrayToMap(args);
        assertNotNull(argumentMap);
        assertThat(argumentMap.size(), is(4));
        assertTrue(argumentMap.get("--init").isEmpty());
        assertThat(argumentMap.get("-u"), is("me"));
        assertTrue(argumentMap.get("--status").isEmpty());
        assertTrue(argumentMap.get("-h").isEmpty());
    }

    @Test
    public void arrayToMapInitLast() {
        String[] args = {"-u", "me", "--init"};
        Map<String, String> argumentMap = App.arrayToMap(args);
        assertNotNull(argumentMap);
        assertThat(argumentMap.size(), is(2));
        assertTrue(argumentMap.get("--init").isEmpty());
        assertThat(argumentMap.get("-u"), is("me"));
    }

    @Test
    public void arrayToMapStatusLast() {
        String[] args = {"-u", "me", "--status"};
        Map<String, String> argumentMap = App.arrayToMap(args);
        assertNotNull(argumentMap);
        assertThat(argumentMap.size(), is(2));
        assertTrue(argumentMap.get("--status").isEmpty());
        assertThat(argumentMap.get("-u"), is("me"));
    }

    @Test
    public void arrayToMapHelpLast() {
        String[] args = {"--init", "-h"};
        Map<String, String> argumentMap = App.arrayToMap(args);
        assertNotNull(argumentMap);
        assertThat(argumentMap.size(), is(2));
        assertTrue(argumentMap.get("--init").isEmpty());
        assertTrue(argumentMap.get("-h").isEmpty());
    }

    @Test
    public void argumentsToAction() {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("--init", "");
        arguments.put("-h", "");
        Arguments action = App.argumentsToAction(arguments);
        assertThat(action, is(Arguments.HELP_ACTION));

        arguments.clear();
        arguments.put("--status", "");
        arguments.put("--init", "");
        action = App.argumentsToAction(arguments);
        assertThat(action, is(Arguments.INIT_ACTION));

        arguments.clear();
        arguments.put("-u", "me");
        arguments.put("--status", "");
        action = App.argumentsToAction(arguments);
        assertThat(action, is(Arguments.STATUS_ACTION));

        arguments.clear();
        arguments.put("-UNKNOWN-", null);
        action = App.argumentsToAction(arguments);
        assertThat(action, is(Arguments.SEND_ACTION));
    }

    @Test
    public void mainUsage() {
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        App.main(new String[]{});
        verifyUsageHeader(stream);
        verify(stream).println(Matchers.argThat(containsString("--init -h")));
        verify(stream).println(Matchers.argThat(containsString("--send -h")));
        verify(stream).println(Matchers.argThat(containsString("--status -h")));
    }

    @Test
    public void mainRegisterUsage() {
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        App.main(new String[]{"-h", "--init"});
        verifyUsageHeader(stream);
        verify(stream).println(Matchers.argThat(containsString("--init")));
        verify(stream).println(Matchers.argThat(containsString("-s:")));
        verify(stream).println(Matchers.argThat(containsString("-u:")));
        verify(stream).println(Matchers.argThat(containsString("-e:")));
        verify(mockExitHelper).exit(eq(0));
    }

    @Test
    public void mainStatusUsage() {
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        App.main(new String[]{"-h", "--status"});
        verifyUsageHeader(stream);
        verify(stream).println(Matchers.argThat(containsString("--status")));
        verify(stream).println(Matchers.argThat(containsString("-s:")));
        verify(stream).println(Matchers.argThat(containsString("-u:")));
        verify(stream).println(Matchers.argThat(containsString("-p:")));
        verify(stream).println(Matchers.argThat(containsString("-id:")));
        verify(mockExitHelper).exit(eq(0));
    }

    @Test
    public void mainSendUsage() {
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        App.main(new String[]{"-h", "--send"});
        verifyUsageHeader(stream);
        verify(stream).println(Matchers.argThat(containsString("--send")));
        verify(stream).println(Matchers.argThat(containsString("-s:")));
        verify(stream).println(Matchers.argThat(containsString("-u:")));
        verify(stream).println(Matchers.argThat(containsString("-p:")));
        verify(stream).println(Matchers.argThat(containsString("-m:")));
        verify(stream).println(Matchers.argThat(containsString("-t:")));
        verify(mockExitHelper).exit(eq(0));
    }
    
    @Test
    public void mainRegisterOkVerificationOk() throws Exception {
        MessengerEngine mockEngine = mock(MessengerEngine.class);
        doNothing().when(mockEngine).startRegistration(anyString(), anyString(), anyString());
        doReturn("password").when(mockEngine).verifyRegistration(anyString(), anyString(), anyString());
        PowerMockito.whenNew(MessengerEngine.class).withNoArguments().thenReturn(mockEngine);
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        BufferedReader mockBR = configureInput("pincode");

        App.main(new String[]{"--init", "-u", "user", "-s", "server", "-e", "email"});
        verify(mockEngine).startRegistration(eq("server"), eq("user"), eq("email"));
        verify(mockBR).readLine();
        verify(mockEngine).verifyRegistration(eq("server"), eq("user"), eq("pincode"));
        verify(stream).println(Matchers.argThat(containsString(": password")));
        verify(mockExitHelper).exit(eq(0));
    }
    
    @Test
    public void mainRegisterNok() throws Exception {
        MessengerEngine mockEngine = mock(MessengerEngine.class);
        doThrow(MessengerEngineException.class).when(mockEngine).startRegistration(anyString(), anyString(), anyString());
        PowerMockito.whenNew(MessengerEngine.class).withNoArguments().thenReturn(mockEngine);
        doThrow(new MessengerEngineException("exit", "test")).when(mockExitHelper).exit(eq(1));
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        try {
            App.main(new String[]{"--init", "-u", "user", "-s", "server", "-e", "email"});
            fail("MessengerEngineException expected!");
        } catch (MessengerEngineException ex) {
            assertEquals("exit", ex.getCode());
        }
        verify(mockEngine).startRegistration(eq("server"), eq("user"), eq("email"));
        verify(mockExitHelper).exit(eq(1));
    }
    
    @Test
    public void mainRegisterOkVerificationNok() throws Exception {
        MessengerEngine mockEngine = mock(MessengerEngine.class);
        doNothing().when(mockEngine).startRegistration(anyString(), anyString(), anyString());
        doThrow(MessengerEngineException.class).when(mockEngine).verifyRegistration(anyString(), anyString(), anyString());
        PowerMockito.whenNew(MessengerEngine.class).withNoArguments().thenReturn(mockEngine);
        doThrow(new MessengerEngineException("exit", "test")).when(mockExitHelper).exit(eq(1));
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        configureInput("pincode");
        try {
            App.main(new String[]{"--init", "-u", "user", "-s", "server", "-e", "email"});
            fail("MessengerEngineException expected!");
        } catch (MessengerEngineException ex) {
            assertEquals("exit", ex.getCode());
        }
        verify(mockExitHelper).exit(eq(1));
    }
    
    @Test
    public void mainSendMessageOk() throws Exception {
        MessengerEngine mockEngine = mock(MessengerEngine.class);
        doReturn("msgId").when(mockEngine).sendMessage(anyString(), anyString(), anyString(), anyString(), anyList());
        PowerMockito.whenNew(MessengerEngine.class).withNoArguments().thenReturn(mockEngine);
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);

        App.main(new String[]{"-u", "user", "-s", "server", "-p", "password", "-m", "message", "-t", "to"});
        verify(mockEngine).sendMessage(eq("server"), eq("user"), eq("password"), eq("message"), anyList());
        verify(stream).println(Matchers.argThat(containsString(": msgId")));
        verify(mockExitHelper).exit(eq(0));
    }
    
    @Test
    public void mainSendMessageNok() throws Exception {
        MessengerEngine mockEngine = mock(MessengerEngine.class);
        doThrow(MessengerEngineException.class).when(mockEngine).sendMessage(anyString(), anyString(), anyString(), anyString(), anyList());
        PowerMockito.whenNew(MessengerEngine.class).withNoArguments().thenReturn(mockEngine);
        doThrow(new MessengerEngineException("exit", "test")).when(mockExitHelper).exit(eq(1));
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        try {
            App.main(new String[]{"-u", "user", "-s", "server", "-p", "password", "-m", "message", "-t", "to"});
            fail("MessengerEngineException expected!");
        } catch (MessengerEngineException ex) {
            assertEquals("exit", ex.getCode());
        }
        verify(mockExitHelper).exit(eq(1));
    }
    
    @Test
    public void mainGetStatusOk() throws Exception {
        MMPStatusReportDetail detail = new MMPStatusReportDetail();
        detail.setMessageId("msgId");
        List<MMPRecipientStatus> recipientList = new ArrayList<>();
        MMPRecipientStatus recipient = new MMPRecipientStatus();
        recipient.setMsisdn("to");
        recipient.setStatusId("statusId");
        recipient.setStatus("status");
        recipientList.add(recipient);
        detail.setRecipients(recipientList);
        List<MMPStatusReportDetail> reports = new ArrayList<>();
        reports.add(detail);

        MessengerEngine mockEngine = mock(MessengerEngine.class);
        doReturn(reports).when(mockEngine).statusReports(anyString(), anyString(), anyString(), anyList());
        PowerMockito.whenNew(MessengerEngine.class).withNoArguments().thenReturn(mockEngine);
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);

        App.main(new String[]{"--status", "-u", "user", "-s", "server", "-p", "password", "-m", "message", "-id", "id"});
        verify(mockEngine).statusReports(eq("server"), eq("user"), eq("password"), anyList());
        verify(stream).format(anyString(), eq("msgId"), eq("to"), eq("statusId"), eq("status"));
        verify(mockExitHelper).exit(eq(0));
    }
    
    @Test
    public void mainGetStatusNok() throws Exception {
        MessengerEngine mockEngine = mock(MessengerEngine.class);
        doThrow(MessengerEngineException.class).when(mockEngine).statusReports(anyString(), anyString(), anyString(), anyList());
        PowerMockito.whenNew(MessengerEngine.class).withNoArguments().thenReturn(mockEngine);
        doThrow(new MessengerEngineException("exit", "test")).when(mockExitHelper).exit(eq(1));
        PrintStream stream = Mockito.mock(PrintStream.class);
        System.setOut(stream);
        try {
            App.main(new String[]{"--status", "-u", "user", "-s", "server", "-p", "password", "-m", "message", "-id", "id"});
            fail("MessengerEngineException expected!");
        } catch (MessengerEngineException ex) {
            assertEquals("exit", ex.getCode());
        }
        verify(mockExitHelper).exit(eq(1));
    }
    
    protected void verifyUsageHeader(PrintStream stream) {
        verify(stream).println(Matchers.argThat(containsString("Usage")));
        verify(stream).println(Matchers.argThat(containsString("-----")));
    }
    
    protected BufferedReader configureInput(String wanted) throws Exception {
        BufferedReader mockBR = mock(BufferedReader.class);
        PowerMockito.whenNew(BufferedReader.class).withAnyArguments().thenReturn(mockBR);
        when(mockBR.readLine()).thenReturn(wanted);
        return mockBR;
    }
}