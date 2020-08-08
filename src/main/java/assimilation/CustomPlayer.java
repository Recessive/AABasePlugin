package assimilation;

import mindustry.entities.type.Player;
import mindustry.game.Team;

public class CustomPlayer{

    public int assimRank;
    public Team lastTeam;
    protected Player player;
    protected int playTime;


    public CustomPlayer(Player player, int assimRank, int playTime){
        this.player = player;
        this.assimRank = assimRank;
        this.lastTeam = player.getTeam();
        this.assimRank = 4;
        this.playTime = playTime;
    }

}