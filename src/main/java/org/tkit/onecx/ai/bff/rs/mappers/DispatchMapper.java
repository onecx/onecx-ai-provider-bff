package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.ChatMessageInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.ChatRequestInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.ConversationInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.RequestContextInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ChatMessageDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ChatRequestDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ConversationDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.RequestContextDTO;

@Mapper
public interface DispatchMapper {

    ChatRequestInternal map(ChatRequestDTO chatRequestDTO);

    RequestContextInternal map(RequestContextDTO requestContextDTO);

    ConversationInternal map(ConversationDTO conversationDTO);

    ChatMessageDTO map(ChatMessageInternal chatMessageInternal);
}
