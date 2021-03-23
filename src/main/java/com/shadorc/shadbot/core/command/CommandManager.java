package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.command.admin.IamCmd;
import com.shadorc.shadbot.command.admin.ManageCoinsCmd;
import com.shadorc.shadbot.command.admin.PruneCmd;
import com.shadorc.shadbot.command.admin.SettingsCmd;
import com.shadorc.shadbot.command.admin.member.*;
import com.shadorc.shadbot.command.currency.*;
import com.shadorc.shadbot.command.game.blackjack.BlackjackCmd;
import com.shadorc.shadbot.command.game.dice.DiceCmd;
import com.shadorc.shadbot.command.game.hangman.HangmanCmd;
import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.command.game.roulette.RouletteCmd;
import com.shadorc.shadbot.command.game.rps.RpsCmd;
import com.shadorc.shadbot.command.game.russianroulette.RussianRouletteCmd;
import com.shadorc.shadbot.command.game.slotmachine.SlotMachineCmd;
import com.shadorc.shadbot.command.game.trivia.TriviaCmd;
import com.shadorc.shadbot.command.hidden.*;
import com.shadorc.shadbot.command.info.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class CommandManager {

    private static CommandManager instance;

    static {
        CommandManager.instance = new CommandManager();
    }

    private final Map<String, BaseCmd> commandsMap;

    private CommandManager() {
        this.commandsMap = CommandManager.initialize(

                // Utility Commands
                //new WeatherCmd(), new MathCmd(), new TranslateCmd(), new WikipediaCmd(), new PollCmd(),
                //new UrbanCmd(), new LyricsCmd(),
                // Fun Commands
                //new ChatCmd(), new ThisDayCmd(), new JokeCmd(), new DtcCmd(),
                // Image Commands
                //new GifCmd(), new ImageCmd(), new WallpaperCmd(), new SuicideGirlsCmd(),
                //new Rule34Cmd(), new XkcdCmd(),
                // Game Commands
                new RpsCmd(), new HangmanCmd(), new TriviaCmd(), new RussianRouletteCmd(),
                new SlotMachineCmd(), new RouletteCmd(), new BlackjackCmd(), new DiceCmd(),
                new LotteryCmd(),
                // Currency Commands
                new CoinsCmd(), new LeaderboardCmd(), new TransferCoinsCmd(), new ShopCmd(), new NumMutesCmd(),
                new BuyMutesCmd(), new GiveWeirdchampCmd(), new WeirdchampLeaderboardCmd(), new WeirdChampsCmd(),
                new FreeCoinsCmd(),
                // Music Commands
                //new PlayCmd(), new PauseCmd(), new StopCmd(), new SkipCmd(), new RepeatCmd(),
                //new BackwardCmd(), new ForwardCmd(), new VolumeCmd(), new NameCmd(),
                //new PlaylistCmd(), new ShuffleCmd(), new ClearCmd(), new BassBoostCmd(),
                // Game Stats Commands
                //new FortniteCmd(), new DiabloCmd(), new CounterStrikeCmd(), new OverwatchCmd(),
                // Info Commands
                new PingCmd(), new InfoCmd(), new UserInfoCmd(), new ServerInfoCmd(),
                new RolelistCmd(), new FeedbackCmd(), new InviteCmd(), new AchievementsCmd(),
                new VoteCmd(),
                // Admin Commands
                new ManageCoinsCmd(), new PruneCmd(), //new KickCmd(), new SoftBanCmd(), new BanCmd(),
                new IamCmd(), new SettingsCmd(), new MuteCmd(), new UnMuteCmd(),
                // Owner Commands
                //new LoggerCmd(), new LeaveGuildCmd(), new GenerateRelicCmd(), new SendMessageCmd(), new ShutdownCmd(),
                //new EnableCommandCmd(), new ManageAchievementsCmd(),
                // Hidden Commands
                new ActivateRelicCmd(), new HelpCmd(), new BaguetteCmd(), new RelicStatusCmd(), new PrefixCmd());
    }

    private static Map<String, BaseCmd> initialize(BaseCmd... cmds) {
        final Map<String, BaseCmd> map = new LinkedHashMap<>();
        for (final BaseCmd cmd : cmds) {
            for (final String name : cmd.getNames()) {
                if (map.putIfAbsent(name, cmd) != null) {
                    DEFAULT_LOGGER.error("Command name collision between {} and {}",
                            name, map.get(name).getClass().getSimpleName());
                }
            }
        }
        DEFAULT_LOGGER.info("{} commands initialized", cmds.length);
        return Collections.unmodifiableMap(map);
    }

    public Map<String, BaseCmd> getCommands() {
        return this.commandsMap;
    }

    public BaseCmd getCommand(String name) {
        return this.commandsMap.get(name);
    }

    public static CommandManager getInstance() {
        return CommandManager.instance;
    }
}
