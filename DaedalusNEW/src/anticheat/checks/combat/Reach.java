package anticheat.checks.combat;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.comphenix.protocol.wrappers.EnumWrappers;

import anticheat.Daedalus;
import anticheat.detections.Checks;
import anticheat.detections.ChecksListener;
import anticheat.detections.ChecksType;
import anticheat.packets.events.PacketUseEntityEvent;
import anticheat.utils.Latency;
import anticheat.utils.MathUtils;
import anticheat.utils.PlayerUtils;
import anticheat.utils.TimerUtils;

@ChecksListener(events = {PlayerMoveEvent.class, EntityDamageByEntityEvent.class, PacketUseEntityEvent.class, PlayerQuitEvent.class})
public class Reach extends Checks {
	
    public static Map<Player, Integer> count = new HashMap();
    public static Map<Player, Map.Entry<Double, Double>> offsets = new HashMap();
    public static Map<Player, Long> reachTicks = new HashMap();
    private Map<Player, Long> reachTicks2 = new HashMap();
    private ArrayList<Player> projectileHit = new ArrayList();

	public Reach() {
		super("Reach", "Reach", ChecksType.COMBAT , 5, Daedalus.getAC(), true);
	}
	
	@Override
	protected void onEvent(Event event) {
		if(event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
	        if (!(e.getDamager() instanceof Player)) {
	            return;
	        }
	        if (!(e.getEntity() instanceof Player)) {
	            return;
	        }
	        Player damager = (Player)e.getDamager();
	        Player player = (Player)e.getEntity();
	        double Reach = MathUtils.trim(2, PlayerUtils.getEyeLocation(damager).distance(player.getEyeLocation()) - 0.32);
	        double Reach2 = MathUtils.trim(2, PlayerUtils.getEyeLocation(damager).distance(player.getEyeLocation()) - 0.32);
	        
	        double Difference;
	        
	        if(Daedalus.getAC().getPing().getTPS() < 17D) {
	        	return;
	        }
	        if (damager.getAllowFlight()) {
	            return;
	        }
	        if (player.getAllowFlight()) {
	            return;
	        }
	        
	        if(!count.containsKey(damager)) {
	        	count.put(damager, 0);
	        }
	        
	        int Count = count.get(damager);
	        long Time = System.currentTimeMillis();
	        double MaxReach = 3.1;
	        double YawDifference = Math.abs(damager.getEyeLocation().getYaw() - player.getEyeLocation().getYaw());
	        double speedToVelocityDif = 0;
	        double velocityDifference2 = Math.abs(damager.getVelocity().length() + player.getVelocity().length());
	        double offsets = 0.0D;
	        double offsetsp = 0.0D;
	        double lastHorizontal = 0.0D;
	        if(this.offsets.containsKey(damager)) {
	        	offsets = ((Double)((Map.Entry)this.offsets.get(damager)).getKey()).doubleValue();
	        	lastHorizontal = ((Double)((Map.Entry)this.offsets.get(damager)).getValue()).doubleValue();
	        }
	        if(this.offsets.containsKey(player)) {
	        	offsetsp = ((Double)((Map.Entry)this.offsets.get(player)).getKey()).doubleValue();
	        }
	        if(Latency.getLag(damager) > 100 || Latency.getLag(player) > 100) {
	        	return;
	        }
	        speedToVelocityDif = Math.abs(offsets - player.getVelocity().length());
	        MaxReach += (YawDifference * 0.001);
	         MaxReach += lastHorizontal * 1.5;
	         MaxReach += speedToVelocityDif * 0.09;
	        if (damager.getLocation().getY() > player.getLocation().getY()) {
	            Difference = damager.getLocation().getY() - player.getLocation().getY();
	            MaxReach += Difference / 2.5;
	        } else if (player.getLocation().getY() > damager.getLocation().getY()) {
	            Difference = player.getLocation().getY() - damager.getLocation().getY();
	            MaxReach += Difference / 2.5;
	        }
	        MaxReach += damager.getWalkSpeed() <= 0.2 ? 0 : damager.getWalkSpeed() - 0.2;
	        for (PotionEffect effect : damager.getActivePotionEffects()) {
	            if (effect.getType().equals(PotionEffectType.SPEED)) {
	                 MaxReach += 0.3D * (effect.getAmplifier() + 1);
	            }
	        }
	        int PingD = Daedalus.getAC().getPing().getPing(damager);
	        int PingP = Daedalus.getAC().getPing().getPing(player);
	        MaxReach += ((PingD + PingP) / 2) * 0.0024;
	        Reach2 -= MathUtils.trim(2, velocityDifference2);
	        Reach2 -= MathUtils.trim(2, offsetsp);
	        if (TimerUtils.elapsed(Time, 10000)) {
	            count.remove(damager);
	            Time = System.currentTimeMillis();
	        }
	        if (Reach > MaxReach) {
	            count.put(damager, Count + 1);
	        } else {
	        	if(Count >= -2) {
	        		count.put(damager, Count - 1);
	        	}
	        }
	        if(Reach2 > 6) {
	        	e.setCancelled(true);
	        }
	        if(Count >= 2 && Reach > MaxReach && Reach < 20.0) {
	        	count.remove(damager);
	        	if(Latency.getLag(player) < 115) {
	        		this.Alert(damager, "(Type B)");
	        		
	        	}
	        	return;
	        }
	        long attackTime = System.currentTimeMillis();
	        if (this.reachTicks.containsKey(damager)) {
	            attackTime = reachTicks.get(damager);
	        }
		}
		if(event instanceof PlayerQuitEvent) {
			PlayerQuitEvent e = (PlayerQuitEvent) event;
	    	if(offsets.containsKey(e.getPlayer())) {
	    		offsets.remove(e.getPlayer());
	    	}
	    	if(count.containsKey(e.getPlayer())) {
	    		count.remove(e.getPlayer());
	    	}
	    	if(reachTicks.containsKey(e.getPlayer())) {
	    		reachTicks.remove(e.getPlayer());
	    	}
		}
		if(event instanceof PlayerMoveEvent) {
			PlayerMoveEvent e = (PlayerMoveEvent) event;
	    	if(e.getFrom().getX() == e.getTo().getX() &&
	    			e.getFrom().getZ() == e.getTo().getZ()) {
	    		return;
	    	}
	    	double OffsetXZ = MathUtils.offset(MathUtils.getHorizontalVector(e.getFrom().toVector()), MathUtils.getHorizontalVector(e.getTo().toVector()));
	    	double horizontal = Math.sqrt(Math.pow(e.getTo().getX() - e.getFrom().getX(), 2.0) + Math.pow(e.getTo().getZ() - e.getFrom().getZ(), 2.0));
	    	this.offsets.put(e.getPlayer(), new AbstractMap.SimpleEntry(Double.valueOf(OffsetXZ), Double.valueOf(horizontal)));
		}
		if(event instanceof PacketUseEntityEvent) {
			PacketUseEntityEvent e = (PacketUseEntityEvent) event;
			 if (e.getAction() != EnumWrappers.EntityUseAction.ATTACK) {
		           return;
		       }
		       if (!(e.getAttacked() instanceof Player)) {
		           return;
		       }
		       if(e.getAttacker().getAllowFlight()) {
		    	   return;
		       }
		       if(Daedalus.getAC().getPing().getTPS() < 17) {
		    	   return;
		       }
		       Player damager = e.getAttacker();
		       Player player = (Player)e.getAttacked();
		       double ydist = Math.abs(damager.getEyeLocation().getY() - player.getEyeLocation().getY());
		       double Reach = MathUtils.trim(2, (PlayerUtils.getEyeLocation(damager).distance(player.getEyeLocation()) - ydist) - 0.32);
		       int PingD = Daedalus.getAC().getPing().getPing(damager);
		       
		       long attackTime = System.currentTimeMillis();
		       if (this.reachTicks2.containsKey(damager)) {
		           attackTime = reachTicks2.get(damager);
		       }
		       double yawdif = Math.abs(damager.getLocation().getYaw() - player.getLocation().getYaw());
		       if(Latency.getLag(damager) > 80 || Latency.getLag(player) > 80) {
		    	   return;
		       }
		       double offsetsp = 0.0D;
		       double lastHorizontal = 0.0D;
		       double offsetsd = 0.0D;
		       if(this.offsets.containsKey(damager)) {
		       	offsetsd = ((Double)((Map.Entry)this.offsets.get(damager)).getKey()).doubleValue();
		       	lastHorizontal = ((Double)((Map.Entry)this.offsets.get(damager)).getValue()).doubleValue();
		       }
		       if(this.offsets.containsKey(player)) {
		          	offsetsp = ((Double)((Map.Entry)this.offsets.get(player)).getKey()).doubleValue();
		          	lastHorizontal = ((Double)((Map.Entry)this.offsets.get(player)).getValue()).doubleValue();
		       }
		       double velocityDifference2 = Math.abs(damager.getVelocity().length() + player.getVelocity().length());
		       Reach -= MathUtils.trim(2, offsetsd);
		       Reach -= MathUtils.trim(2, offsetsp);
		       double maxReach2 = 3.12;
		       if(yawdif < 90) {
		    	   maxReach2+= 0.38;
		       }
		       maxReach2 += lastHorizontal * 1.09;
		       
		       maxReach2 += Daedalus.getAC().getPing().getPing(damager) * 0.0021;
		       if(Reach > maxReach2 && TimerUtils.elapsed(attackTime, 1100) && !projectileHit.contains(player)) {
		    	   
		        this.Alert(damager, "(Type C)");
		       }
		       reachTicks2.put(damager, TimerUtils.nowlong());
		       projectileHit.remove(player);
		}
		if(event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
		   	if(!(e.getDamager() instanceof Player)) {
		   		return;
		   	}
		   	if(e.getCause() != DamageCause.PROJECTILE) {
		   		return;
		   	}
		   	
		   	Player player = (Player) e.getDamager();
		   	
		   	this.projectileHit.add(player);
		}
	}
}