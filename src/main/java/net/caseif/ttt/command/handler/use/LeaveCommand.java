/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2019, Max Roncace <me@caseif.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.caseif.ttt.command.handler.use;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.command.handler.CommandHandler;
import net.caseif.ttt.util.config.ConfigKey;
import net.caseif.ttt.util.config.OperatingMode;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.helper.platform.BungeeHelper;

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand extends CommandHandler {

    public LeaveCommand(CommandSender sender, String[] args) {
        super(sender, args);
    }

    @Override
    public void handle() {
        if (TTTCore.config.get(ConfigKey.OPERATING_MODE) == OperatingMode.DEDICATED
                && !(sender.hasPermission("ttt.admin") && args.length >= 2 && args[1].equalsIgnoreCase("force"))) {
            if (BungeeHelper.hasSupport() && !TTTCore.config.get(ConfigKey.RETURN_SERVER).isEmpty()) {
                TTTCore.locale.getLocalizable("info.personal.bungee.connecting").withPrefix(Color.INFO).sendTo(sender);
                trySendForceLeaveTip();
                BungeeHelper.sendPlayerToReturnServer((Player) sender);
                return;
            } else {
                TTTCore.locale.getLocalizable("error.round.leave-dedicated").withPrefix(Color.ALERT).sendTo(sender);
                trySendForceLeaveTip();
                return;
            }
        }

        Optional<Challenger> ch = TTTCore.mg.getChallenger(((Player) sender).getUniqueId());
        if (ch.isPresent()) {
            String roundName = ch.get().getRound().getArena().getDisplayName();
            ch.get().removeFromRound();
            TTTCore.locale.getLocalizable("info.personal.arena.leave.success").withPrefix(Color.INFO)
                    .withReplacements(Color.EM + roundName + Color.INFO).sendTo(sender);
        } else {
            TTTCore.locale.getLocalizable("error.round.outside").withPrefix(Color.ALERT).sendTo(sender);
        }
    }

    private void trySendForceLeaveTip() {
        if (sender.hasPermission("ttt.admin")) {
            TTTCore.locale.getLocalizable("error.round.leave-dedicated-force").withPrefix(Color.ALERT)
                    .withReplacements(Color.EM + "/ttt leave force").sendTo(sender);
        }
    }

}
