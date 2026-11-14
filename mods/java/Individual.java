public class Individual extends Actor {
   String m_sIndividualName = "No_name";
   AdverbSpace m_advSpace;
   Context m_currContext = null;
   boolean m_bDone = true;
   boolean m_bAnimationFinished = true;
   Verb m_initVerb = null;
   GraphObject m_currGO = null;
   Verb m_currVerb = null;
   Transition m_currTrans = null;
   Transition m_selectTrans = null;
   Animation m_currAnim = null;
   Sound m_currSound = null;
   int m_currSoundId;
   boolean m_bIsPlaying = false;

   public void Start() {
      this.SetPhysicsActive(true);
      this.m_currVerb = this.m_initVerb;
      this.m_currGO = this.m_initVerb;
      this.StartVerb();
   }

   public String GetIndividualName() {
      return this.m_sIndividualName;
   }

   public void ChangeActivity(String sActivity) {
      if (!sActivity.equals(this.m_currVerb.GetName())) {
         this.DisableCallMethod("PlayStartAnim");
         this.DisableCallMethod("PlayInnerAnim");
         this.DisableCallMethod("PlayEndAnim");
         this.m_selectTrans = this.m_currVerb.GetSuccessor(sActivity);
         this.PlayEndAnim();
      }
   }

   public void ChangeActivityRand() {
      this.DisableCallMethod("PlayStartAnim");
      this.DisableCallMethod("PlayInnerAnim");
      this.DisableCallMethod("PlayEndAnim");
      int nSize = this.m_currVerb.GetSuccCount();
      if (nSize == 0) {
         this.m_selectTrans = null;
      } else {
         if (nSize == 1) {
            this.m_selectTrans = this.m_currVerb.GetSuccessor(0);
         } else {
            this.m_selectTrans = this.m_currVerb.GetSuccessor(GameObject.RandomInt(nSize - 1));
         }

         GameObject.LogLine("RAND ");
         this.PlayEndAnim();
      }
   }

   public void OnAnimEnd(int iAnimID) {
      this.m_bAnimationFinished = false;
   }

   void PlayStartAnim() {
      if (this.m_currVerb.m_startAnims != null) {
         this.m_currAnim = (Animation)this.m_currVerb.m_startAnims.FindBest(this.m_currContext);
         if (this.m_currAnim != null) {
            if (this.m_currAnim.m_fFrequency > 0.0F) {
               this.SetAnimFreq(this.m_currAnim.GetName(), this.m_currAnim.m_fFrequency);
            }

            float fNextTime = this.GetAnimLength(this.m_currAnim.GetName());
            fNextTime -= this.m_currVerb.m_fMorphTime;
            if (this.m_currVerb.m_bPhysic) {
               this.SetLocalSpeed(new Vector(0.0F, 0.0F, this.m_currAnim.m_fSpeed));
            }

            this.PlayAnim(this.m_currAnim.GetName(), 1);
            this.m_bAnimationFinished = false;
            this.DisableCallMethod("PlayInnerAnim");
            this.CallMethodIn("PlayInnerAnim", fNextTime);
         } else {
            this.PlayInnerAnim();
         }
      } else {
         this.PlayInnerAnim();
      }
   }

   public void VerbExit() {
      this.m_bAnimationFinished = true;
      this.EndGraphObject();
      if (this.m_selectTrans != null) {
         this.m_currGO = this.m_selectTrans;
         this.m_currTrans = this.m_selectTrans;
         this.StartTransition();
      }
   }

   public void SetDestDir(Vector vDir, float fSpeed) {
      this.SetMaxAngSpeed(fSpeed);
      this.SetDestDir(vDir);
      GameObject.LogLine("SET DEST DIR");
   }

   public void SetIndividualName(String sName) {
      this.m_sIndividualName = sName;
      this.SetName(sName);
   }

   void PlayCyclic() {
      if (!this.m_bIsPlaying) {
         this.PlaySound(this.m_currSoundId);
         this.m_bIsPlaying = true;
      }

      this.DisableCallMethod("EndSound");
      this.CallMethodIn("EndSound", this.m_currSound.m_fLength);
   }

   String GetActivity() {
      return this.m_currVerb.m_sName;
   }

   public void SetAnimFreq(String sSeqName, float fFreq) {
      if (fFreq > 0.0F) {
         super.SetAnimFreq(sSeqName, fFreq);
      }
   }

   boolean IsContext(String sAdverb, String sValue) {
      return this.m_currContext.IsValue(sAdverb, sValue, this.m_advSpace);
   }

   void SetContext(String sAdverb, String sValue) {
      this.m_currContext.SetValue(sAdverb, sValue, this.m_advSpace);
   }

   void InitMeshAnimations(Species species) {
   }

   public void StartTransition() {
      this.StartGraphObject();
      if (this.m_currTrans.m_animations != null) {
         this.m_currAnim = (Animation)this.m_currTrans.m_animations.FindBest(this.m_currContext);
         if (this.m_currAnim != null) {
            float fNextTime = this.GetAnimLength(this.m_currAnim.GetName());
            fNextTime -= this.m_currTrans.m_fMorphTime;
            if (this.m_currTrans.m_bPhysic) {
               this.SetSpeed(new Vector(0.0F, 0.0F, this.m_currAnim.m_fSpeed));
            }

            this.PlayAnim(this.m_currAnim.GetName(), 1);
            this.DisableCallMethod("TransExit");
            this.CallMethodIn("TransExit", fNextTime);
         } else {
            this.TransExit();
         }
      } else {
         this.TransExit();
      }
   }

   public void TransExit() {
      this.EndGraphObject();
      this.m_currGO = this.m_currTrans.m_verb;
      this.m_currVerb = this.m_currTrans.m_verb;
      this.StartVerb();
   }

   void StartSound() {
      if (this.m_currGO.m_sounds != null) {
         this.m_currSound = (Sound)this.m_currGO.m_sounds.FindBest(this.m_currContext);
         // [mod] en ligne : l'hôte diffuse les cris des joueurs, le client ne lance pas les siens
         if (this.m_currSound != null && !ModNet.OnVoice(this, this.m_currSound.GetName(), this.m_currSound.m_fRangeMin, this.m_currSound.m_fRangeMax)) {
            this.m_currSound = null;
         }

         if (this.m_currSound != null) {
            this.m_currSoundId = this.AttachSound(this.m_currSound.GetName(), this.m_currSound.m_fRangeMin, this.m_currSound.m_fRangeMax);
            this.m_bIsPlaying = true;
            if (this.m_currSound.m_bIsCyclic) {
               if (this.m_currSound.m_bIsCyclic && this.m_currSound.m_fPause > 0.0F) {
                  this.DisableCallMethod("EndSound");
                  this.CallMethodIn("EndSound", this.m_currSound.m_fLength);
                  this.DisableCallMethod("PlayCyclic");
                  this.CallMethodEvery("PlayCyclic", this.m_currSound.m_fPause + this.m_currSound.m_fLength);
               }
            } else {
               this.CallMethodIn("EndSound", this.m_currSound.m_fLength);
            }
         }
      }
   }

   void PlayInnerAnim() {
      this.m_bAnimationFinished = true;
      if (this.m_currVerb.m_innerAnims != null) {
         this.m_currAnim = (Animation)this.m_currVerb.m_innerAnims.FindBest(this.m_currContext);
         if (this.m_currAnim != null) {
            if (this.m_currAnim.m_fFrequency > 0.0F) {
               this.SetAnimFreq(this.m_currAnim.GetName(), this.m_currAnim.m_fFrequency);
            }

            float fNextTime = this.GetAnimLength(this.m_currAnim.GetName());
            fNextTime -= this.m_currVerb.m_fMorphTime;
            this.CallMethodIn("SetAnimationFinished", fNextTime);
            if (this.m_currVerb.m_bPhysic) {
               this.SetLocalSpeed(new Vector(0.0F, 0.0F, this.m_currAnim.m_fSpeed));
            }

            this.PlayAnim(this.m_currAnim.GetName(), -1);
            this.m_bAnimationFinished = false;
         }
      }

      this.m_bDone = true;
      if (!this.m_currVerb.m_bStable) {
         this.ChangeActivityRand();
      }
   }

   void PlayEndAnim() {
      this.m_bAnimationFinished = true;
      if (this.m_currVerb.m_endAnims != null) {
         this.m_currAnim = (Animation)this.m_currVerb.m_endAnims.FindBest(this.m_currContext);
         if (this.m_currAnim != null) {
            if (this.m_currAnim.m_fFrequency > 0.0F) {
               this.SetAnimFreq(this.m_currAnim.GetName(), this.m_currAnim.m_fFrequency);
            }

            float fNextTime = this.GetAnimLength(this.m_currAnim.GetName());
            if (this.m_selectTrans != null) {
               float fMorphTime = this.m_selectTrans.m_fMorphTime;
            } else {
               float var4 = 0.0F;
            }

            fNextTime -= this.m_currVerb.m_fMorphTime;
            if (this.m_currVerb.m_bPhysic) {
               this.SetLocalSpeed(new Vector(0.0F, 0.0F, this.m_currAnim.m_fSpeed));
            }

            this.PlayAnim(this.m_currAnim.GetName(), 1);
            this.m_bAnimationFinished = false;
            this.DisableCallMethod("VerbExit");
            this.CallMethodIn("VerbExit", fNextTime);
         } else {
            this.VerbExit();
         }
      } else {
         this.VerbExit();
      }
   }

   void ClearSound() {
      this.DisableCallMethod("EndSound");
      this.DisableCallMethod("PlayCyclic");
      if (this.m_currSound != null) {
         this.DetachAllSounds();
      }
   }

   public void StartVerb() {
      this.StartGraphObject();
      this.PlayStartAnim();
   }

   boolean Done() {
      return this.m_bDone;
   }

   void SetNoDone() {
      this.m_bDone = false;
   }

   public void SetAnimationFinished() {
      this.DisableCallMethod("SetAnimationFinished");
      this.m_bAnimationFinished = true;
   }

   public void ChangeContext() {
      if (this.m_currVerb != null) {
         this.SetAnimMorphTime(this.m_currVerb.m_fMorphTime);
      }

      this.ClearSound();
      this.StartSound();
      this.PlayInnerAnim();
   }

   public void SetSpecies(Species species) {
      this.m_advSpace = species.m_advSpace;
      this.m_initVerb = species.GetVerb(0);
      this.m_currContext = new Context(this.m_advSpace.GetDimension());
      if (species.m_sMeshFileName != null) {
         this.LoadMesh(species.m_sMeshFileName);
         GameObject.CrashLog("Individual.SetSpecies loaded mesh " + species.m_sMeshFileName);
      }

      this.InitMeshAnimations(species);
      this.ClearFlag(4);
      this.ClearFlag(2);
   }

   public void EndGraphObject() {
      this.ClearSound();
   }

   boolean IsActivity(String sActivity) {
      return this.m_currVerb.m_sName.equals(sActivity);
   }

   public void OnCreate() {
      super.OnCreate();
   }

   void UpdateSpeed() {
      if (this.m_currAnim != null) {
         this.SetLocalSpeed(new Vector(0.0F, 0.0F, this.m_currAnim.m_fSpeed));
      } else {
         GameObject.LogLine("UpdateSpeedError: currAnim==null ind: " + this + " m_currVerb: " + this.m_currVerb.m_sName);
      }
   }

   void EndSound() {
      if (!this.m_currSound.m_bIsCyclic) {
         this.DetachSound(this.m_currSoundId);
      } else if (this.m_bIsPlaying) {
         this.StopSound(this.m_currSoundId);
         this.m_bIsPlaying = false;
      }
   }

   String GetCurrContext(String sAdverb) {
      return this.m_currContext.GetValue(sAdverb, this.m_advSpace);
   }

   public void StartGraphObject() {
      this.StartSound();
      if (this.m_currGO.m_bPhysic) {
         this.SetPhysicsType(this.m_currGO.m_nPhysicType);
      } else {
         this.SetSpeed(new Vector(0.0F, 0.0F, 0.0F));
      }
   }
}
