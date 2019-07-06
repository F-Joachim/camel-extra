/**************************************************************************************
 https://camel-extra.github.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.


 You should have received a copy of the GNU Lesser General Public
 License along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 02110-1301, USA.

 http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ***************************************************************************************/
package org.apacheextras.camel.component.wmq;

import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.MQTopic;
import com.ibm.mq.headers.MQHeaderList;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.tika.parser.txt.CharsetDetector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WMQConsumerTest {

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private Processor processor;

    @Mock
    private WMQEndpoint wmqEndpoint;

    @Mock
    private WMQComponent wmqComponent;

    @Mock
    private MQQueueManager mqQueueManager;

    @Mock
    private MQTopic mqDestination;

    @Mock
    private Function<MQMessage, MQHeaderList> headerListFactory;

    @Mock
    private MQHeaderList mqHeaderList;

    private WMQConsumer consumer;

    @Captor
    private ArgumentCaptor<Object> bodyCaptor;

    @Captor
    private ArgumentCaptor<Class<?>> bodyClassCaptor;

    private static final String MESSAGE_IN_QUEUE = "I am the message";

    @Before
    public void before() throws Exception {
        when(exchange.getIn()).thenReturn(this.message);
        doReturn(exchange).when(this.wmqEndpoint).createExchange();
        doReturn(wmqComponent).when(this.wmqEndpoint).getComponent();
        doReturn("topic:test").when(this.wmqEndpoint).getDestinationName();
        doReturn(mqQueueManager).when(this.wmqComponent).getQueueManager(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        doReturn(mqDestination).when(mqQueueManager).accessTopic(anyString(), anyString(), anyInt(), anyString(), anyString());
        doReturn(mqHeaderList).when(headerListFactory).apply(any(MQMessage.class));
        doReturn(-1).when(mqHeaderList).indexOf("MQRFH2");
        doNothing().when(this.message).setBody(bodyCaptor.capture(), bodyClassCaptor.capture());
        doAnswer((arg) -> {
            final MQMessage mqMessage = (MQMessage) arg.getArguments()[0];
            mqMessage.write(MESSAGE_IN_QUEUE.getBytes());
            return null;
        }).when(mqDestination).get(any(MQMessage.class), any(MQGetMessageOptions.class));
        this.consumer = new WMQConsumer(this.wmqEndpoint, this.processor, this.headerListFactory);
    }

    @Test
    public void pool_noOutputConfiguration_setBodyAsUTF8String() throws Exception {
        this.consumer.poll();
        final String value = (String) bodyCaptor.getValue();
        CharsetDetector charsetDetector = new CharsetDetector();
        charsetDetector.setText(value.getBytes());
        assertThat(charsetDetector.detect().getName()).isEqualTo("UTF-8");
    }

    @Test
    public void pool_bodyTypeAsBytes_setBodyAsByteBuffer() throws Exception {
        when(wmqEndpoint.getBodyType()).thenReturn("bytes");
        this.consumer.poll();

        assertThat(bodyCaptor.getValue()).isInstanceOf(ByteBuffer.class);
        assertThat(bodyClassCaptor.getValue()).isEqualTo(ByteBuffer.class);
    }


}
