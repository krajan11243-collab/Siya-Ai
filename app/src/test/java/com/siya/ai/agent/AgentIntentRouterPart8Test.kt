package com.siya.ai.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentIntentRouterPart8Test {
    @Test
    fun launchAndSearchCommandsRouteOffline() {
        val open = AgentIntentRouter.route("open YouTube")!!
        assertEquals(AgentIntent.LaunchApp("com.google.android.youtube"), open.intent)
        assertFalse(open.confirmationRequired)

        val search = AgentIntentRouter.route("search latest Android news")!!
        assertEquals(AgentIntent.WebSearch("latest Android news"), search.intent)
        assertFalse(search.confirmationRequired)
    }

    @Test
    fun dangerousActionsRequireExplicitConfirmation() {
        val call = AgentIntentRouter.route("call 9876543210")!!
        assertTrue(call.confirmationRequired)
        assertEquals(AgentIntent.Call("9876543210"), call.intent)

        val message = AgentIntentRouter.route("message Rahul: I am coming")!!
        assertTrue(message.confirmationRequired)
        assertEquals(AgentIntent.SendMessage("Rahul", "I am coming"), message.intent)
    }

    @Test
    fun malformedOrUnknownCommandsAreNotGuessed() {
        assertNull(AgentIntentRouter.route("delete everything"))
        assertNull(AgentIntentRouter.route("message Rahul"))
    }
}
